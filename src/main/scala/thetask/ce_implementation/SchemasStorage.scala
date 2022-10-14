package thetask
package ce_implementation

import better.files.File
import cats.data.EitherT
import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option._
import io.circe.generic.semiauto.deriveCodec
import io.circe.syntax.EncoderOps
import io.circe.{Codec, Printer, jawn}
import thetask.SchemaIdToMd5.hashSchemaID


trait SchemasStorage[F[_]] {
  def get(schemaId: SchemaId): F[Either[SchemasStorageError, Option[JsonSchema]]]

  def store(value: JsonSchema): F[Either[SchemasStorageError, Unit]]
}

class FileBasedStorage[F[_] : Sync](parentDir: File) extends SchemasStorage[F] {
  implicit val jsonSchemaCodec: Codec[JsonSchema] = deriveCodec

  def fileOfASchema(schemaId: SchemaId): F[Either[Throwable, File]] =
    EitherT(Sync[F].delay(hashSchemaID(schemaId).map(_ + ".json"))).flatMap(x => EitherT(Sync[F].delay(Either.catchNonFatal(parentDir / x)))).value

  override def get(schemaId: SchemaId) = {
    EitherT(fileOfASchema(schemaId))
      .leftMap[SchemasStorageError](err => CantReadSchemaFromStorage(schemaId, s"Can't generate md5hash $err"))
      .flatMapF { schemaFile =>
        if (schemaFile.isRegularFile) {
          val parsedEither = jawn.decode[JsonSchema](schemaFile.contentAsString).leftMap[SchemasStorageError](err => CantReadSchemaFromStorage(schemaId, err.toString)).map(_.some)
          parsedEither.pure[F]
        } else none[JsonSchema].asRight[SchemasStorageError].pure[F]
      }
      .value
  }

  override def store(value: JsonSchema) = {
    EitherT(fileOfASchema(value.id))
      .leftMap[SchemasStorageError](err => CantReadSchemaFromStorage(value.id, s"Can't generate md5hash $err"))
      .flatMapF { schemaFile =>
        if (schemaFile.isRegularFile)
          (SchemaAlreadyExists(value.id): SchemasStorageError).asLeft[Unit].pure[F]
        else {
          EitherT(Sync[F].delay(Either.catchNonFatal {
            schemaFile.createFile()
            schemaFile.overwrite(value.asJson.printWith(Printer.spaces2))
          }))
            .leftMap[SchemasStorageError](err => CantStoreSchema(value.id, err.toString))
            .void
            .value
        }
      }.value
  }
}
