package io.sumislawski.jsonvs.core

import cats.MonadThrow
import cats.effect.Sync
import cats.syntax.all._
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.github.fge.jsonschema.core.report.ProcessingReport
import com.github.fge.jsonschema.main.JsonSchemaFactory
import io.circe.{Json, Printer}
import io.sumislawski.jsonvs.core.ValidationResult._

import scala.jdk.CollectionConverters.IteratorHasAsScala

// We might want to split it into two services (one for managing the known schemas, and one for running the validation) if it grows significantly
class SchemaValidationService[F[_] : MonadThrow] private(storage: SchemaStorage[F],
                                                         jackson: ObjectMapper,
                                                         jsonSchemaFactory: JsonSchemaFactory,
                                                        ) {

  def getSchema(id: SchemaId): F[Schema] = storage.getSchema(id)

  def createSchema(id: SchemaId, schema: Schema): F[Unit] = storage.createSchema(id, schema)

  def validateJson(schemaId: SchemaId, document: Json): F[ValidationResult] = for {
    schema <- storage.getSchema(schemaId)
    schemaAsJsonNode <- parseWithJackson(schema.json.toString())
    jsonSchema <- MonadThrow[F].catchNonFatal(jsonSchemaFactory.getJsonSchema(schemaAsJsonNode))
    documentAsJsonNode <- parseWithJackson(document.printWith(Printer.noSpaces.copy(dropNullValues = true)))
    validationReport <- MonadThrow[F].catchNonFatal(jsonSchema.validate(documentAsJsonNode))
  } yield toValidationResult(validationReport)

  private def parseWithJackson(s: String): F[JsonNode] =
    MonadThrow[F].catchNonFatal(jackson.readTree(s))

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
}
