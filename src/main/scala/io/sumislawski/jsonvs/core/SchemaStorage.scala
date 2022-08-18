package io.sumislawski.jsonvs.core

trait SchemaStorage[F[_]] {

  def getSchema(id: SchemaId): F[Option[Schema]]

  def createSchema(id: SchemaId, schema: Schema): F[Unit]

}
