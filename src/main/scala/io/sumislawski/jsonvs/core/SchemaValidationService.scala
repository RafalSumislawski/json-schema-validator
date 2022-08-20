package io.sumislawski.jsonvs.core

import cats.MonadThrow
import cats.effect.Sync
import cats.syntax.all._
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.github.fge.jsonschema.core.report.ProcessingReport
import com.github.fge.jsonschema.main.{JsonSchema, JsonSchemaFactory}
import io.circe.Printer
import io.sumislawski.jsonvs.core.SchemaValidationService.{InvalidJsonDocument, InvalidSchema}
import io.sumislawski.jsonvs.core.ValidationResult._
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.jdk.CollectionConverters.IteratorHasAsScala

// We might want to split it into two services (one for managing the known schemas, and one for running the validation) if it grows significantly
class SchemaValidationService[F[_] : Sync] private(storage: SchemaStorage[F],
                                                         jackson: ObjectMapper,
                                                         jsonSchemaFactory: JsonSchemaFactory,
                                                        ) {

  private val logger = Slf4jLogger.getLogger[F]

  def downloadSchema(id: SchemaId): F[Schema] =
    logger.info(s"Downloading schema [$id].") >>
      storage.getSchema(id)

  def uploadSchema(id: SchemaId, schema: Schema): F[Unit] = for {
    _ <- logger.info(s"Uploading schema [$id].")
    _ <- readSchema(schema).adaptError { t => new InvalidSchema(t) }
    _ <- storage.createSchema(id, schema)
  } yield ()

  def validateDocument(schemaId: SchemaId, document: Document): F[ValidationResult] = for {
    _ <- logger.info(s"Validating document against schema [$schemaId].")
    schema <- storage.getSchema(schemaId)
    jsonSchema <- readSchema(schema).adaptError { t => new InvalidSchema(t) }
    cleanedDocument <- clean(document).adaptError { t => new InvalidJsonDocument(t) }
    documentAsJsonNode <- parseWithJackson(cleanedDocument.toString).adaptError { t => new InvalidJsonDocument(t) }
    validationReport <- MonadThrow[F].catchNonFatal(jsonSchema.validate(documentAsJsonNode))
  } yield toValidationResult(validationReport)

  private def readSchema(schema: Schema): F[JsonSchema] = for {
    schemaAsJsonNode <- parseWithJackson(schema.toString())
    jsonSchema <- MonadThrow[F].catchNonFatal(jsonSchemaFactory.getJsonSchema(schemaAsJsonNode))
  } yield jsonSchema

  private def parseWithJackson(s: String): F[JsonNode] =
    MonadThrow[F].catchNonFatal(jackson.readTree(s))

  private def clean(document: Document): F[Document] =
    io.circe.parser.parse(document.toString).liftTo[F]
      .map(_.printWith(Printer.noSpaces.copy(dropNullValues = true)))
      .map(Document)

  private def toValidationResult(report: ProcessingReport): ValidationResult =
    if (report.isSuccess) Valid
    else Invalid(
      report.iterator().asScala
        .maxByOption(_.getLogLevel)
        .fold("unknown validation issues")(_.getMessage)
    )

}

object SchemaValidationService {
  def apply[F[_] : Sync](storage: SchemaStorage[F]): F[SchemaValidationService[F]] = for {
    jackson <- Sync[F].delay(new ObjectMapper())
    jsonSchemaFactory <- Sync[F].delay(JsonSchemaFactory.byDefault())
  } yield new SchemaValidationService(storage, jackson, jsonSchemaFactory)

  class InvalidSchema(cause: Throwable) extends Exception("Invalid JSON schema", cause)

  class InvalidJsonDocument(cause: Throwable) extends Exception("Invalid JSON document", cause)
}
