package io.sumislawski.jsonvs.infrastructure.filestorage

import cats.Applicative
import cats.effect.Sync
import io.sumislawski.jsonvs.core.SchemaStorage

class LocalFileSystemSchemaStorage[F[_] : Sync] extends SchemaStorage[F] {
  override def getSchema(id: SchemaStorage.SchemaId): F[Option[SchemaStorage.Schema]] =
    Applicative[F].pure(None)

  override def createSchema(id: SchemaStorage.SchemaId, schema: SchemaStorage.Schema): F[Unit] =
    Applicative[F].unit
}
