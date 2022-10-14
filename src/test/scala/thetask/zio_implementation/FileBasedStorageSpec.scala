package thetask
package zio_implementation

import better.files.File
import cats.syntax.either._
import zio.test.Assertion._
import zio.test._
import zio.{ZIO, ZLayer}

import scala.util.Try


object FileBasedStorageSpec extends ZIOSpecDefault {

  val tmpDirL = ZLayer(ZIO.fromEither(Either.catchNonFatal(File.newTemporaryDirectory())))
  val storageL = tmpDirL >>> ZLayer.fromFunction(SchemasStorage.makeFileBased(_)).flatten

  val jsonSchema1 = JsonSchema(SchemaId("schema1"), """ {"some-key": "some-value"}""")
  val jsonSchema2 = JsonSchema(SchemaId("schema2"), """ {"some-key": "another-value"}""")

  lazy val spec = suite("FileBasedStorage")(
    test("should store files") {
      for {
        tmpDir <- ZIO.service[File]
        _ <- SchemasStorage.store(jsonSchema1)
        retrieved <- SchemasStorage.retrieve(jsonSchema1.id)
        filesUnderTmpDir <- ZIO.fromTry(Try(tmpDir.children.size))
      } yield assert(retrieved)(equalTo(Some(jsonSchema1))) &&
        assert(filesUnderTmpDir)(equalTo(1))
    }.provide(tmpDirL and storageL),

    test("should forbid override") {
      for {
        _ <- SchemasStorage.store(jsonSchema1)
        res <- SchemasStorage.store(jsonSchema1).exit
      } yield assert(res)(fails(isSubtype[SchemaAlreadyExists](anything)))
    }.provide(storageL),

    test("should return None if schema missing") {
      for {
        _ <- SchemasStorage.store(jsonSchema1)
        res <- SchemasStorage.retrieve(jsonSchema2.id)
      } yield assert(res)(isNone)
    }.provide(storageL)
  )
}
