package io.sumislawski.jsonvs

import io.circe.Json

package object core {

  case class SchemaId(id: String) extends AnyVal

  case class Schema(json: Json)

}
