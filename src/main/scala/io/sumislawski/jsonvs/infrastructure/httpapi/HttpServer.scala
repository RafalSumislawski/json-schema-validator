package io.sumislawski.jsonvs.infrastructure.httpapi

import cats.Monad
import cats.effect.Resource
import org.http4s.HttpRoutes

object HttpServer {

  def apply[F[_] : Monad](routes: HttpRoutes[F]): Resource[F, Unit] =
    Resource.unit

}
