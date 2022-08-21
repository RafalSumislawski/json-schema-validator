package io.sumislawski.jsonvs.infrastructure.httpapi

import cats.data.Kleisli
import cats.effect.{Async, Resource}
import cats.syntax.all._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Connection
import org.http4s.server.ServiceErrorHandler
import org.http4s.{Headers, HttpApp, HttpRoutes, MessageFailure}
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.net.InetSocketAddress
import scala.util.control.NonFatal

class HttpServer[F[_] : Async] private(routes: HttpRoutes[F]) extends Http4sDsl[F] {

  private val logger = Slf4jLogger.getLogger[F]

  private def resource: Resource[F, Unit] =
    BlazeServerBuilder[F]
      .bindSocketAddress(new InetSocketAddress(8080)) // TODO make the port configurable. 80 won't work out-f-the-box on UNIX
      .withServiceErrorHandler(errorHandler)
      .withHttpApp(orNotFound(routes))
      .resource
      .void

  // Very similar to the default error handler except responses are JSONs
  private def errorHandler: ServiceErrorHandler[F] = {
    req => {
      case mf: MessageFailure =>
        logger.debug(mf)(s"""Message failure handling request: ${req.method} ${req.pathInfo} from ${req.remoteAddr.getOrElse("<unknown>")}""") >>
          mf.toHttpResponse[F](req.httpVersion)
            .withEntity(StatusResponse(Status.Error, mf.getMessage()))
            .pure[F]
      case NonFatal(t) =>
        logger.error(t)(s"""Error servicing request: ${req.method} ${req.pathInfo} from ${req.remoteAddr.getOrElse("<unknown>")}""") >>
          InternalServerError(StatusResponse(Status.Error, "Internal server error"), Headers(Connection.close))
    }
  }

  private def orNotFound(routes: HttpRoutes[F]): HttpApp[F] =
    Kleisli(a => routes.run(a).getOrElseF(NotFound(StatusResponse(Status.Error, "Not found"))))

}

object HttpServer {
  def apply[F[_] : Async](routes: HttpRoutes[F]): Resource[F, Unit] =
    new HttpServer[F](routes).resource
}
