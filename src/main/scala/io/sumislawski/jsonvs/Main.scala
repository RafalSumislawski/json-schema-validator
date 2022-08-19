package io.sumislawski.jsonvs

import cats.effect.{IO, IOApp}
import fs2.io.file.Path
import io.sumislawski.jsonvs.core.SchemaValidationService
import io.sumislawski.jsonvs.infrastructure.filestorage.LocalFileSystemSchemaStorage
import io.sumislawski.jsonvs.infrastructure.httpapi.{HttpServer, SchemaValidationRoutes}
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp.Simple {

  private val logger = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] = for {
    _ <- logger.info("Starting JSON validation service.")
    storage <- LocalFileSystemSchemaStorage[IO](Path("schemaStorage"))
    service = new SchemaValidationService[IO](storage)
    routes = new SchemaValidationRoutes[IO](service)
    _ <- HttpServer[IO](routes.routes).useForever
  } yield ()
}
