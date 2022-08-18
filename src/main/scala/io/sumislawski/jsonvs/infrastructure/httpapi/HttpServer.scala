package io.sumislawski.jsonvs.infrastructure.httpapi

import cats.effect.{Async, Resource}
import cats.syntax.all._
import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder

import java.net.InetSocketAddress

object HttpServer {

  def apply[F[_] : Async](routes: HttpRoutes[F]): Resource[F, Unit] =
    BlazeServerBuilder[F]
      .bindSocketAddress(new InetSocketAddress(80)) // TODO make the port configurable. 80 won't work out-f-the-box on UNIX
      //      .withServiceErrorHandler() // TODO errors as JSONs
      .withHttpApp(routes.orNotFound) // TODO not found as JSON
      .resource
      .void

}
