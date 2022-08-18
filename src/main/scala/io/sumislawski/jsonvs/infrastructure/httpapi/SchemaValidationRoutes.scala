package io.sumislawski.jsonvs.infrastructure.httpapi

import cats.Monad
import cats.effect.IO
import io.sumislawski.jsonvs.core.SchemaValidationService
import org.http4s.HttpRoutes
import org.typelevel.log4cats.slf4j.Slf4jLogger

class SchemaValidationRoutes[F[_] : Monad](service: SchemaValidationService[F]) {

  private val logger = Slf4jLogger.getLogger[IO]

  def routes: HttpRoutes[F] = HttpRoutes.empty[F]

}
