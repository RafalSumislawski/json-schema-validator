package io.sumislawski.jsonvs.infrastructure.httpapi

import io.circe.generic.semiauto.deriveEncoder
import io.circe.{Encoder, Printer}
import io.sumislawski.jsonvs.core.SchemaId
import org.http4s.EntityEncoder

case class StatusResponse(action: Option[String], id: Option[String], status: Status, message: Option[String] = None)

object StatusResponse {

  def apply(status: Status, message: String): StatusResponse =
    StatusResponse(None, None, status, Some(message))

  def apply(action: String, id: String, status: Status): StatusResponse =
    StatusResponse(Some(action), Some(id), status, None)

  def apply(action: String, id: String, status: Status, message: String): StatusResponse =
    StatusResponse(Some(action), Some(id), status, Some(message))

  implicit def statusResponseEntityEncoder[F[_]]: EntityEncoder[F, StatusResponse] =
    org.http4s.circe.jsonEncoderWithPrinterOf[F, StatusResponse](Printer.spaces2.copy(dropNullValues = true))

  implicit lazy val statusResponseEncoder: Encoder[StatusResponse] = deriveEncoder

  implicit lazy val schemaIdEncoder: Encoder[SchemaId] = Encoder.encodeString.contramap[SchemaId](_.toString)
}

sealed trait Status

object Status {
  case object Success extends Status

  case object Error extends Status

  implicit lazy val statusEncoder: Encoder[Status] = Encoder.encodeString.contramap[Status](_.toString.toLowerCase)
}

