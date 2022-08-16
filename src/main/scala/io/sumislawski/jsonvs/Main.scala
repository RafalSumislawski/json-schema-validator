package io.sumislawski.jsonvs

import cats.effect.{IO, IOApp}
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp.Simple {

  private val logger = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] =
    logger.info("Hello!")
}
