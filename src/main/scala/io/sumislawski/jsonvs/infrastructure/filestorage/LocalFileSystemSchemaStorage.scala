package io.sumislawski.jsonvs.infrastructure.filestorage

import cats.Applicative
import cats.effect.Sync
import io.sumislawski.jsonvs.core.{Schema, SchemaId, SchemaStorage}

class LocalFileSystemSchemaStorage[F[_] : Sync] extends SchemaStorage[F] {
  override def getSchema(id: SchemaId): F[Option[Schema]] =
    Applicative[F].pure(None)

  override def createSchema(id: SchemaId, schema: Schema): F[Unit] =
    Applicative[F].unit
}
