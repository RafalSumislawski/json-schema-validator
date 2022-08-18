package io.sumislawski.jsonvs.infrastructure.httpapi

import cats.Monad
import cats.effect.IO
import io.sumislawski.jsonvs.core.SchemaValidationService
import org.http4s.HttpRoutes
import org.typelevel.log4cats.slf4j.Slf4jLogger
import cats.effect._
import cats.syntax.all._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.circe._
import io.circe.literal._

class SchemaValidationRoutes[F[_] : Monad](service: SchemaValidationService[F]) extends Http4sDsl[F] {

  private val logger = Slf4jLogger.getLogger[IO]

  def routes: HttpRoutes[F] = HttpRoutes.of{
    case r @ POST -> Root / "schema" / schemaId =>
      Created(json"""{"action": "uploadSchema", "id": "config-schema", "status": "success"}""")
    case GET -> Root / "schema" / schemaId =>
      Ok()
    case r @ POST -> Root / "validate" / schemaId =>
      Ok(json"""{"action": "validateDocument", "id": "config-schema", "status": "success"}""")
  }

}
