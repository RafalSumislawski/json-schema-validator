package io.sumislawski.jsonvs.infrastructure.httpapi

import cats.effect.Async
import cats.syntax.all._
import io.circe.generic.semiauto._
import io.circe.literal._
import io.circe.{Encoder, Json, Printer}
import io.sumislawski.jsonvs.core.SchemaStorage.{SchemaAlreadyExists, SchemaNotFound}
import io.sumislawski.jsonvs.core.{Schema, SchemaId, SchemaValidationService}
import io.sumislawski.jsonvs.infrastructure.httpapi.SchemaValidationRoutes.{Status, StatusResponse}
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityEncoder, HttpRoutes, MalformedMessageBodyFailure}
import org.typelevel.log4cats.slf4j.Slf4jLogger

class SchemaValidationRoutes[F[_] : Async](service: SchemaValidationService[F]) extends Http4sDsl[F] {

  private val logger = Slf4jLogger.getLogger[F]

  private implicit def jsonEncoderWithoutNullsOf[A: Encoder]: EntityEncoder[F, A] =
    org.http4s.circe.jsonEncoderWithPrinterOf[F, A](Printer.spaces2.copy(dropNullValues = true))

  import org.http4s.circe.jsonDecoder

  def routes: HttpRoutes[F] = HttpRoutes.of {
    case r@POST -> Root / "schema" / schemaId =>
      SchemaId(schemaId).liftTo[F].flatMap { id =>
        r.as[Json]
          .flatMap(jsonBody => service.createSchema(id, Schema(jsonBody)))
          .attempt
          .flatMap {
            case Right(_) => Created(StatusResponse("uploadSchema", id, Status.Success))
            case Left(_: MalformedMessageBodyFailure) => BadRequest(StatusResponse("uploadSchema", id, Status.Error, Some("Invalid JSON")))
            case Left(t: SchemaAlreadyExists) => Conflict(StatusResponse("uploadSchema", id, Status.Error, Some(t.getMessage)))
            case Left(t) => InternalServerError(StatusResponse("uploadSchema", id, Status.Error, Some("Failed to process the request"))) // TODO log
          }
      }

    case GET -> Root / "schema" / schemaId =>
      SchemaId(schemaId).liftTo[F].flatMap { id =>
        service.getSchema(id)
          .attempt
          .flatMap {
            case Right(schema) => Ok(schema.json) // TODO consider defining encoder&decoder for Schema
            case Left(t: SchemaNotFound) => NotFound(StatusResponse("downloadSchema", id, Status.Error, Some(t.getMessage)))
            case Left(t) => InternalServerError(StatusResponse("downloadSchema", id, Status.Error, Some("Failed to process the request"))) // TODO log
          }
      }

    case r@POST -> Root / "validate" / schemaId =>
      SchemaId(schemaId).liftTo[F].flatMap { id =>
        Ok(json"""{"action": "validateDocument", "id": "config-schema", "status": "success"}""")
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
