package io.sumislawski.jsonvs.core

import io.sumislawski.jsonvs.core.SchemaStorage._

trait SchemaStorage[F[_]] {

  def getSchema(id: SchemaId): F[Option[Schema]]

  def createSchema(id: SchemaId, schema: Schema): F[Unit]

}

object SchemaStorage {
  case class SchemaId(id: String) extends AnyVal

  case class Schema()
}
