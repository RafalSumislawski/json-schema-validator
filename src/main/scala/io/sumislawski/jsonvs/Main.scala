package io.sumislawski.jsonvs

import cats.effect.{ExitCode, IO, IOApp}
import fs2.io.file.Path
import io.sumislawski.jsonvs.core.SchemaValidationService
import io.sumislawski.jsonvs.infrastructure.filestorage.LocalFileSystemSchemaStorage
import io.sumislawski.jsonvs.infrastructure.httpapi.{HttpServer, SchemaValidationRoutes}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import cats.syntax.all._

import java.net.InetSocketAddress

object Main extends IOApp {

  private val logger = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] = for {
    _ <- logger.info("Starting JSON validation service.")
    customBindAddress <- args.headOption.traverse(s => IO(new InetSocketAddress(s.toInt)))
    bindAddress = customBindAddress.getOrElse(new InetSocketAddress(80))
    storage <- LocalFileSystemSchemaStorage[IO](Path("schemaStorage"))
    service <- SchemaValidationService[IO](storage)
    routes = new SchemaValidationRoutes[IO](service)
    _ <- HttpServer[IO](routes.routes, bindAddress)
      .useForever
  } yield ExitCode.Success
}
