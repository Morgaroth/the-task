package thetask

import better.files.File
import cats.syntax.either._
import cats.syntax.option._
import io.circe.generic.semiauto.deriveCodec
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import io.circe.{Codec, Printer}
import thetask.FileBasedStorage.hashSchemaID
import zio.{IO, Ref, ZIO, ZLayer}

import java.math.BigInteger
import java.security.MessageDigest

trait SchemasStorageError

case class SchemaAlreadyExists(schemaId: SchemaId) extends SchemasStorageError

case class CantReadSchemaFromStorage(schemaId: SchemaId, error: String) extends SchemasStorageError

case class CantStoreSchema(schemaId: SchemaId, error: String) extends SchemasStorageError

trait SchemasStorage {
  def get(schemaId: SchemaId): IO[SchemasStorageError, Option[JsonSchema]]

  def store(value: JsonSchema): IO[SchemasStorageError, Unit]
}

object SchemasStorage {
  def makeInMem(initData: List[JsonSchema]): ZLayer[Any, Nothing, SchemasStorage] =
    ZLayer.fromZIO(Ref.Synchronized.make(initData.map(x => x.id -> x).toMap).map(new InMemSchemasStorage(_)))

  def makeFileBased(parentDir: File): ZLayer[Any, Throwable, SchemasStorage] =
    ZLayer.fromZIO(for {
      _ <- ZIO.fromEither(Either.catchNonFatal(parentDir.createDirectoryIfNotExists(createParents = true)))
      _ <- ZIO.cond(parentDir.isDirectory, (), new IllegalArgumentException(s"${parentDir.path} is not a dir"))
      // possibly more checks
    } yield new FileBasedStorage(parentDir)
    )

  val inMem = makeInMem(Nil)

  def retrieve(schemaId: SchemaId): ZIO[SchemasStorage, SchemasStorageError, Option[JsonSchema]] = ZIO.service[SchemasStorage].flatMap(_.get(schemaId))

  def store(value: JsonSchema): ZIO[SchemasStorage, SchemasStorageError, Unit] = ZIO.service[SchemasStorage].flatMap(_.store(value))
}

class InMemSchemasStorage(
                           ref: Ref.Synchronized[Map[SchemaId, JsonSchema]]
                         ) extends SchemasStorage {
  override def get(schemaId: SchemaId): IO[SchemasStorageError, Option[JsonSchema]] = {
    ref.get.map(_.get(schemaId))
  }

  override def store(value: JsonSchema): IO[SchemasStorageError, Unit] = {
    ref.updateZIO { underlying =>
      if (underlying.contains(value.id)) ZIO.fail(SchemaAlreadyExists(value.id))
      else ZIO.succeed(underlying.updated(value.id, value))
    }
  }
}

object FileBasedStorage {
  def hashSchemaID(schemaId: SchemaId) = ZIO.fromEither(Either.catchNonFatal {
    val hasher = MessageDigest.getInstance("MD5")
    hasher.update(schemaId.value.getBytes)
    val bigInt = new BigInteger(1, hasher.digest())
    bigInt.toString(16).toUpperCase
  })
}

class FileBasedStorage(parentDir: File) extends SchemasStorage {
  implicit val jsonSchemaCodec: Codec[JsonSchema] = deriveCodec

  def fileOfASchema(schemaId: SchemaId) =
    hashSchemaID(schemaId).map(_ + ".json").map(parentDir./)

  override def get(schemaId: SchemaId) = {
    fileOfASchema(schemaId)
      .mapError(err => CantReadSchemaFromStorage(schemaId, s"Can't generate md5hash $err"))
      .flatMap { schemaFile =>
        if (schemaFile.isRegularFile)
          ZIO.fromEither(decode[JsonSchema](schemaFile.contentAsString)).mapError(err => CantReadSchemaFromStorage(schemaId, err.toString)).map(_.some)
        else ZIO.none
      }
  }

  override def store(value: JsonSchema) = {
    fileOfASchema(value.id)
      .mapError(err => CantStoreSchema(value.id, s"Can't generate md5hash $err"))
      .flatMap { schemaFile =>
        if (schemaFile.isRegularFile)
          ZIO.fail(SchemaAlreadyExists(value.id))
        else {
          ZIO.fromEither(Either.catchNonFatal {
            schemaFile.createFile()
            schemaFile.overwrite(value.asJson.printWith(Printer.spaces2))
          })
            .mapError[SchemasStorageError](err => CantStoreSchema(value.id, err.toString))
            .unit
        }
      }
  }
}