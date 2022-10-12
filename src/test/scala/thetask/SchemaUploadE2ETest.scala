package thetask

import io.circe.syntax._
import io.circe.{Printer, jawn}
import sttp.client3.circe._
import sttp.client3.{UriContext, basicRequest}
import zhttp.http.{HttpData, Method, Request, URL}
import zio.ZIO
import zio.test.Assertion._
import zio.test.{assert, _}

object SchemaUploadE2ETest extends ZIOSpecDefault {

  val validPostRequest = Request(
    url = URL.fromString("http://example.com/schema/test-schema").getOrElse(???),
    method = Method.POST,
    data = HttpData.fromString(Printer.noSpaces.print(Map("some-field" -> "some-value").asJson))
  ).withContentType("application/json")

  val invalidJsonUpload = Request(
    url = URL.fromString("http://example.com/schema/test-schema").getOrElse(???),
    method = Method.POST,
    data = HttpData.fromString("""{"key": "unclosedString}""")
  ).withContentType("application/json")

  val validGetSchema = Request(
    url = URL.fromString("http://example.com/schema/test-schema").getOrElse(???),
    method = Method.GET,
  )

  val invalidGetSchema = Request(
    url = URL.fromString("http://example.com/schema/non-existing-schema").getOrElse(???),
    method = Method.GET,
  )

  lazy val spec = suite("SchemaUploadE2ETest")(
    test("should accept correct upload") {
      for {
        res <- Boot.api(validPostRequest)
        parsed <- Utils.extractJsonMap(res)
      } yield assert(res.status.code)(equalTo(201)) &&
        assert(parsed)(equalTo(Map(
          "id" -> "test-schema",
          "action" -> "uploadSchema",
          "status" -> "success")))
    }.provide(SchemasStorage.inMem),

    test("should reject invalid json") {
      for {
        res <- Boot.api(invalidJsonUpload)
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
        res <- Boot.api(validPostRequest)
        createRespCode = res.status.code
        retrieve <- Boot.api(validGetSchema)
        parsed <- Utils.extractJsonMap(retrieve)
      } yield assert(createRespCode)(equalTo(201)) &&
        assert(retrieve.status.code)(equalTo(200)) &&
        assert(parsed)(equalTo(Map("some-field" -> "some-value")))
    }.provide(SchemasStorage.inMem),

    test("should respond with meaning response for non existing schema") {
      for {
        res <- Boot.api(invalidGetSchema)
        parsed <- Utils.extractJsonMap(res)
      } yield assert(res.status.code)(equalTo(404)) &&
        assert(parsed)(equalTo(Map(
          "id" -> "non-existing-schema",
          "action" -> "getSchema",
          "status" -> "error",
          "message" -> "Schema 'non-existing-schema' not found in the storage")))
    }.provide(SchemasStorage.inMem)
  )
}
