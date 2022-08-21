package io.sumislawski.jsonvs.infrastructure.httpapi

import cats.effect.Async
import cats.syntax.all._
import io.sumislawski.jsonvs.core.SchemaId.IllegalSchemaId
import io.sumislawski.jsonvs.core.SchemaStorage.{SchemaAlreadyExists, SchemaNotFound}
import io.sumislawski.jsonvs.core.SchemaValidationService.{InvalidJsonDocument, InvalidSchema}
import io.sumislawski.jsonvs.core.ValidationResult.{Invalid, Valid}
import io.sumislawski.jsonvs.core.{Document, Schema, SchemaId, SchemaValidationService}
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityEncoder, HttpRoutes}
import org.typelevel.log4cats.slf4j.Slf4jLogger

class SchemaValidationRoutes[F[_] : Async](service: SchemaValidationService[F]) extends Http4sDsl[F] {

  private val logger = Slf4jLogger.getLogger[F]

  // We need this here (as opposed to having it as an import),
  // because otherwise the jsonEncoderWithoutNullsOf will be used for String and that's not what we want.
  private implicit val stringEncoder: EntityEncoder[F, String] = EntityEncoder.stringEncoder[F]

  def routes: HttpRoutes[F] = HttpRoutes.of {
    case r@POST -> Root / "schema" / schemaId =>
      SchemaId(schemaId).liftTo[F]
        .flatMap { id =>
          r.as[String].flatMap(body => service.uploadSchema(id, Schema(body)))
        }
        .onError { t => logger.error(t)(s"Failed to upload schema [$schemaId].") }
        .attempt
        .flatMap {
          case Right(_) => Created(StatusResponse("uploadSchema", schemaId, Status.Success))
          case Left(t: IllegalSchemaId) => BadRequest(StatusResponse("uploadSchema", schemaId, Status.Error, t.getMessage))
          case Left(t: InvalidSchema) => BadRequest(StatusResponse("uploadSchema", schemaId, Status.Error, t.getMessage))
          case Left(t: SchemaAlreadyExists) => Conflict(StatusResponse("uploadSchema", schemaId, Status.Error, t.getMessage))
          case Left(_) => InternalServerError(StatusResponse("uploadSchema", schemaId, Status.Error, "Failed to process the request"))
        }

    case GET -> Root / "schema" / schemaId =>
      SchemaId(schemaId).liftTo[F]
        .flatMap { id => service.downloadSchema(id) }
        .onError { t => logger.error(t)(s"Failed to download schema [$schemaId].") }
        .attempt
        .flatMap {
          case Right(schema) => Ok(schema.toString)
          case Left(t: IllegalSchemaId) => BadRequest(StatusResponse("downloadSchema", schemaId, Status.Error, t.getMessage))
          case Left(t: SchemaNotFound) => NotFound(StatusResponse("downloadSchema", schemaId, Status.Error, t.getMessage))
          case Left(_) => InternalServerError(StatusResponse("downloadSchema", schemaId, Status.Error, "Failed to process the request"))
        }

    case r@POST -> Root / "validate" / schemaId =>
      SchemaId(schemaId).liftTo[F]
        .flatMap { id =>
          r.as[String].flatMap { document => service.validateDocument(id, Document(document)) }
        }
        .onError { t => logger.error(t)(s"Failed to validate JSON against schema [$schemaId].") }
        .attempt
        .flatMap {
          case Right(Valid) => Ok(StatusResponse("validateDocument", schemaId, Status.Success))
          case Right(Invalid(message)) => UnprocessableEntity(StatusResponse("validateDocument", schemaId, Status.Error, message))
          case Left(t: IllegalSchemaId) => BadRequest(StatusResponse("validateDocument", schemaId, Status.Error, t.getMessage))
          case Left(t: InvalidSchema) => UnprocessableEntity(StatusResponse("validateDocument", schemaId, Status.Error, t.getMessage))
          case Left(t: InvalidJsonDocument) => UnprocessableEntity(StatusResponse("validateDocument", schemaId, Status.Error, t.getMessage))
          case Left(t: SchemaNotFound) => NotFound(StatusResponse("validateDocument", schemaId, Status.Error, t.getMessage))
          case Left(_) => InternalServerError(StatusResponse("validateDocument", schemaId, Status.Error, "Failed to process the request"))
        }
  }
}
