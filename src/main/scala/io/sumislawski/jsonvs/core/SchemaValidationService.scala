package io.sumislawski.jsonvs.core

import cats.{Applicative, Monad}

// We might want to split it into two services (one for managing the known schemas, and one for running the validation) if it grows significantly
class SchemaValidationService[F[_] : Monad](storage: SchemaStorage[F]) {

  def getSchema(id: SchemaId): F[Option[Schema]] = storage.getSchema(id)

  def createSchema(id: SchemaId, schema: Schema): F[Unit] = storage.createSchema(id, schema)

  def validateJson(schemaId: SchemaId, json: String): F[Unit] = Applicative[F].unit

}
