package thetask
package zio_implementation

import io.circe.Printer
import io.circe.syntax._
import zhttp.http.{HttpData, Method, Request, URL}
import zio.test.Assertion._
import zio.test._

object SchemaUploadE2ETest extends ZIOSpecDefault {


  def registerSchemaReq(schemaId: SchemaId, data: String) = {
    URL.fromString(s"http://example.com/schema/${schemaId.value}").toIO.map { url =>
      Request(
        url = url,
        method = Method.POST,
        data = HttpData.fromString(data)
      ).withContentType("application/json")
    }
  }

  def getSchemaReq(schemaId: SchemaId) = {
    URL.fromString(s"http://example.com/schema/${schemaId.value}").toIO.map { url =>
      Request(url = url)
    }
  }

  val validPostRequest = registerSchemaReq(SchemaId("test-schema"), Printer.noSpaces.print(Map("some-field" -> "some-value").asJson))
  val invalidJsonUpload = registerSchemaReq(SchemaId("test-schema"), """{"key": "unclosedString}""")

  val validGetSchema = getSchemaReq(SchemaId("test-schema"))
  val invalidGetSchema = getSchemaReq(SchemaId("non-existing-schema"))

  lazy val spec = suite("SchemaUploadE2ETest")(
    test("should accept correct upload") {
      for {
        res <- validPostRequest.flatMap(Boot.api)
        parsed <- Utils.extractJsonMap(res)
      } yield assert(res.status.code)(equalTo(201)) &&
        assert(parsed)(equalTo(Map(
          "id" -> "test-schema",
          "action" -> "uploadSchema",
          "status" -> "success")))
    }.provide(SchemasStorage.inMem),

    test("should reject invalid json") {
      for {
        res <- invalidJsonUpload.flatMap(Boot.api)
        parsed <- Utils.extractJsonMap(res)
      } yield assert(res.status.code)(equalTo(400)) &&
        assert(parsed)(equalTo(Map(
          "id" -> "test-schema",
          "action" -> "uploadSchema",
          "status" -> "error",
          "message" -> "Provided json for json schema cannot be parsed: io.circe.ParsingFailure: exhausted input")))
    }.provide(SchemasStorage.inMem),

    test("should provide already published schema") {
      for {
        res <- validPostRequest.flatMap(Boot.api)
        createRespCode = res.status.code
        retrieve <- validGetSchema.flatMap(Boot.api)
        parsed <- Utils.extractJsonMap(retrieve)
      } yield assert(createRespCode)(equalTo(201)) &&
        assert(retrieve.status.code)(equalTo(200)) &&
        assert(parsed)(equalTo(Map("some-field" -> "some-value")))
    }.provide(SchemasStorage.inMem),

    test("should respond with meaning response for non existing schema") {
      for {
        res <- invalidGetSchema.flatMap(Boot.api)
        parsed <- Utils.extractJsonMap(res)
      } yield assert(res.status.code)(equalTo(404)) &&
        assert(parsed)(equalTo(Map(
          "id" -> "non-existing-schema",
          "action" -> "getSchema",
          "status" -> "error",
          "message" -> "Schema 'non-existing-schema' not found in the storage")))
    }.provide(SchemasStorage.inMem),
    test("should return sensible response on overriding") {
      for {
        res1 <- validPostRequest.flatMap(Boot.api)
        res2 <- validPostRequest.flatMap(Boot.api)
        parsed <- Utils.extractJsonMap(res2)
      } yield assert(res1.status.code)(equalTo(201)) &&
        assert(res2.status.code)(equalTo(400)) &&
        assert(parsed)(equalTo(Map(
          "id" -> "test-schema",
          "action" -> "uploadSchema",
          "status" -> "error",
          "message" -> "Schema 'test-schema' already exists")))
    }.provide(SchemasStorage.inMem)
  )
}
