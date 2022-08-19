package io.sumislawski.jsonvs.core

trait SchemaStorage[F[_]] {

  def getSchema(id: SchemaId): F[Schema]

  def createSchema(id: SchemaId, schema: Schema): F[Unit]

}

object SchemaStorage {
  class SchemaNotFound(id: SchemaId) extends Exception(s"Schema [$id] doesn't exist")

  class SchemaAlreadyExists(id: SchemaId) extends Exception(s"Schema [$id] already exists.")
}