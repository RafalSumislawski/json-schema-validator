package io.sumislawski.jsonvs.infrastructure.httpapi

import cats.effect.Async
import cats.syntax.all._
import io.circe.generic.semiauto._
import io.circe.{Encoder, Printer}
import io.sumislawski.jsonvs.core.SchemaStorage.{SchemaAlreadyExists, SchemaNotFound}
import io.sumislawski.jsonvs.core.SchemaValidationService.{InvalidJsonDocument, InvalidSchema}
import io.sumislawski.jsonvs.core.ValidationResult.{Invalid, Valid}
import io.sumislawski.jsonvs.core.{Document, Schema, SchemaId, SchemaValidationService}
import io.sumislawski.jsonvs.infrastructure.httpapi.SchemaValidationRoutes.{Status, StatusResponse}
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityEncoder, HttpRoutes}
import org.typelevel.log4cats.slf4j.Slf4jLogger

class SchemaValidationRoutes[F[_] : Async](service: SchemaValidationService[F]) extends Http4sDsl[F] {

  private val logger = Slf4jLogger.getLogger[F]

  // We need this here (as opposed to having it as an import),
  // because otherwise the jsonEncoderWithoutNullsOf will be used for String and that's not what we want.
  private implicit val stringEncoder: EntityEncoder[F, String] = EntityEncoder.stringEncoder[F]

  private implicit def jsonEncoderWithoutNullsOf[A: Encoder]: EntityEncoder[F, A] =
    org.http4s.circe.jsonEncoderWithPrinterOf[F, A](Printer.spaces2.copy(dropNullValues = true))

  def routes: HttpRoutes[F] = HttpRoutes.of {
    case r@POST -> Root / "schema" / schemaId =>
      SchemaId(schemaId).liftTo[F].flatMap { id =>
        r.as[String]
          .flatMap(body => service.createSchema(id, Schema(body)))
          .attempt
          .flatMap {
            case Right(_) => Created(StatusResponse("uploadSchema", id, Status.Success))
            case Left(t: InvalidSchema) => BadRequest(StatusResponse("uploadSchema", id, Status.Error, Some(t.getMessage)))
            case Left(t: SchemaAlreadyExists) => Conflict(StatusResponse("uploadSchema", id, Status.Error, Some(t.getMessage)))
            case Left(_) => InternalServerError(StatusResponse("uploadSchema", id, Status.Error, Some("Failed to process the request"))) // TODO log
          }
      }

    case GET -> Root / "schema" / schemaId =>
      SchemaId(schemaId).liftTo[F].flatMap { id =>
        service.getSchema(id)
          .attempt
          .flatMap {
            case Right(schema) => Ok(schema.toString)
            case Left(t: SchemaNotFound) => NotFound(StatusResponse("downloadSchema", id, Status.Error, Some(t.getMessage)))
            case Left(_) => InternalServerError(StatusResponse("downloadSchema", id, Status.Error, Some("Failed to process the request"))) // TODO log
          }
      }

    case r@POST -> Root / "validate" / schemaId =>
      SchemaId(schemaId).liftTo[F].flatMap { id =>
        r.as[String]
          .flatMap { document => service.validateJson(id, Document(document)) }
          .attempt
          .flatMap {
            case Right(Valid) => Ok(StatusResponse("validateDocument", id, Status.Success))
            case Right(Invalid(message)) => UnprocessableEntity(StatusResponse("validateDocument", id, Status.Error, Some(message)))
            case Left(t: InvalidSchema) => UnprocessableEntity(StatusResponse("validateDocument", id, Status.Error, Some(t.getMessage)))
            case Left(t: InvalidJsonDocument) => UnprocessableEntity(StatusResponse("validateDocument", id, Status.Error, Some(t.getMessage)))
            case Left(t: SchemaNotFound) => NotFound(StatusResponse("validateDocument", id, Status.Error, Some(t.getMessage)))
            case Left(_) => InternalServerError(StatusResponse("validateDocument", id, Status.Error, Some("Failed to process the request"))) // TODO log
          }
      }
  }
}

object SchemaValidationRoutes {
  case class StatusResponse(action: String, id: SchemaId, status: Status, message: Option[String] = None)

  implicit lazy val statusResponseEncoder: Encoder[StatusResponse] = deriveEncoder

  sealed trait Status

  object Status {
    case object Success extends Status

    case object Error extends Status
  }

  implicit lazy val statusEncoder: Encoder[Status] = Encoder.encodeString.contramap[Status](_.toString.toLowerCase)

  implicit lazy val schemaIdEncoder: Encoder[SchemaId] = Encoder.encodeString.contramap[SchemaId](_.toString)
}
