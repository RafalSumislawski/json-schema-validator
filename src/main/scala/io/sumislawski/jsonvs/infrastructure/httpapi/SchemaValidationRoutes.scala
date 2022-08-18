package io.sumislawski.jsonvs.infrastructure.httpapi

import cats.effect.{Async, IO}
import cats.syntax.all._
import io.circe.generic.extras.semiauto.deriveUnwrappedEncoder
import io.circe.generic.semiauto._
import io.circe.literal._
import io.circe.{Encoder, Json}
import io.sumislawski.jsonvs.core.{Schema, SchemaId, SchemaValidationService}
import io.sumislawski.jsonvs.infrastructure.httpapi.SchemaValidationRoutes.{Status, StatusResponse}
import org.http4s.{HttpRoutes, MalformedMessageBodyFailure}
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.slf4j.Slf4jLogger

class SchemaValidationRoutes[F[_] : Async](service: SchemaValidationService[F]) extends Http4sDsl[F] {

  private val logger = Slf4jLogger.getLogger[IO]

  def routes: HttpRoutes[F] = HttpRoutes.of {
    case r@POST -> Root / "schema" / SchemaIdVar(id) =>
      r.as[Json]
        .flatMap(jsonBody => service.createSchema(id, Schema(jsonBody)))
        .attempt
        .flatMap{
          case Right(_) => Created(StatusResponse("uploadSchema", id, Status.Success))
          case Left(_: MalformedMessageBodyFailure) => BadRequest(StatusResponse("uploadSchema", id, Status.Error, Some("Invalid JSON")))
//          case Left(t: Something) => Conflict(StatusResponse("uploadSchema", id, Status.Error, Some("Schema with this ID already exists")))// TODO
          case Left(t) => InternalServerError(StatusResponse("uploadSchema", id, Status.Error, Some(t.getMessage)))
        }

    case GET -> Root / "schema" / schemaId =>
      Ok()
    case r@POST -> Root / "validate" / schemaId =>
      Ok(json"""{"action": "validateDocument", "id": "config-schema", "status": "success"}""")
  }

  object SchemaIdVar {
    def unapply(str: String): Option[SchemaId] = Some(SchemaId(str))
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

  implicit lazy val schemaIdEncoder: Encoder[SchemaId] = deriveUnwrappedEncoder
}
