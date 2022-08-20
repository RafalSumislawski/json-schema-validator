package io.sumislawski.jsonvs.infrastructure.filestorage

import cats.effect.std.Semaphore
import cats.effect.{Async, Sync}
import cats.syntax.all._
import fs2.io.file.{Files, NoSuchFileException, Path}
import io.sumislawski.jsonvs.core.SchemaStorage.{SchemaAlreadyExists, SchemaNotFound}
import io.sumislawski.jsonvs.core.{Schema, SchemaId, SchemaStorage}
import org.typelevel.log4cats.slf4j.Slf4jLogger

class LocalFileSystemSchemaStorage[F[_] : Sync : Files] private(semaphore: Semaphore[F], storageDirectory: Path)
  extends SchemaStorage[F] {

  private val logger = Slf4jLogger.getLogger[F]

  override def getSchema(id: SchemaId): F[Schema] =
    semaphore.permit.use { _ =>
      logger.debug(s"Trying to read schema [$id].") >>
        Files[F].readUtf8(storageDirectory / id.id) // TODO handle nonexistent file
          .compile.foldMonoid
          .map(Schema)
          .adaptError { case t: NoSuchFileException => new SchemaNotFound(id) }
    }

  override def createSchema(id: SchemaId, schema: Schema): F[Unit] =
    semaphore.permit.use { _ =>
      Files[F].exists(storageDirectory / id.id).flatMap {
        case true => new SchemaAlreadyExists(id).raiseError
        case false => writeSchemaToFile(id, schema)
      }
    }

  private def writeSchemaToFile(id: SchemaId, schema: Schema): F[Unit] =
    makeSureStorageDirectoryExists() >>
      logger.debug(s"Writing schema [$id].") >>
      fs2.Stream(schema.toString())
        .through(fs2.text.utf8.encode)
        .through(Files[F].writeAll(storageDirectory / id.id))
        .compile.drain

  private def makeSureStorageDirectoryExists(): F[Unit] =
    Files[F].createDirectories(storageDirectory)

}

object LocalFileSystemSchemaStorage {

  def apply[F[_] : Async : Files](storageDirectory: Path): F[LocalFileSystemSchemaStorage[F]] = for {
    _ <- Slf4jLogger.getLogger[F].info(s"Starting LocalFileSystemSchemaStorage in [$storageDirectory].")
    semaphore <- Semaphore[F](1)
  } yield new LocalFileSystemSchemaStorage[F](semaphore, storageDirectory)
}
