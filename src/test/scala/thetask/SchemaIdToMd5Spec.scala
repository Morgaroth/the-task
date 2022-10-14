package thetask

import zio.ZIO
import zio.test.Assertion._
import zio.test._

object SchemaIdToMd5Spec extends ZIOSpecDefault {
  lazy val spec = suite("SchemaIdToMd5")(
    test("should generate hash as the author expects") {
      for {
        hash <- ZIO.fromEither(SchemaIdToMd5.hashSchemaID(SchemaId("should generate hash as the author expects")))
      } yield assert(hash)(equalTo("96718B5616499A7E0249E5061361A1F4"))
    },
    test("should generate hash as the author expects 2") {
      for {
        hash <- ZIO.fromEither(SchemaIdToMd5.hashSchemaID(SchemaId("config-schema")))
      } yield assert(hash)(equalTo("22E5A544608E1303DB8E7AA4D96608FF"))
    }
  )

}
