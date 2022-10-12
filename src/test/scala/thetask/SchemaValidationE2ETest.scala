package thetask

import zhttp.http.{HttpData, Method, Request, URL}
import zio.test.Assertion._
import zio.test.{assert, _}

import scala.io.Source

object SchemaValidationE2ETest extends ZIOSpecDefault {

  private val schemaId = SchemaId("config-schema")
  val prefilledStorage = SchemasStorage.make(
    Map(schemaId -> JsonSchema(schemaId, Source.fromResource("config-schema.json").mkString("")))
  )

  def validateReq(schemaId: SchemaId, resourceName: String) = {
    URL.fromString(s"http://example.com/validate/${schemaId.value}").toIO.map { url =>
      Request(
        url = url,
        method = Method.POST,
        data = HttpData.fromString(Source.fromResource(resourceName).mkString)
      ).withContentType("application/json")
    }
  }

  lazy val spec = suite("SchemaValidation")(
    test("should return proper response on valid data") {
      for {
        res <- validateReq(schemaId, "valid-json-test.json").flatMap(Boot.api)
        parsed <- Utils.extractJsonMap(res)
      } yield assert(res.status.code)(equalTo(200)) &&
        assert(parsed)(equalTo(Map(
          "id" -> "config-schema",
          "action" -> "validateSchema",
          "status" -> "success"
        )))
    }.provide(prefilledStorage),

    test("should reject invalid json payload") {
      for {
        res <- validateReq(schemaId, "unparseable-json-test.json").flatMap(Boot.api)
        parsed <- Utils.extractJsonMap(res)
      } yield assert(res.status.code)(equalTo(400)) &&
        assert(parsed)(equalTo(Map(
          "id" -> "config-schema",
          "action" -> "validateSchema",
          "status" -> "error",
          "message" -> "Provided json for input json cannot be parsed: io.circe.ParsingFailure: exhausted input")))
    }.provide(prefilledStorage),

    test("should reject not matching json with hints about validation failures") {
      for {
        res <- validateReq(schemaId, "not-matching-json-test.json").flatMap(Boot.api)
        parsed <- Utils.extractJsonMap(res)
      } yield assert(res.status.code)(equalTo(400)) &&
        assert(parsed)(equalTo(Map(
          "id" -> "config-schema",
          "action" -> "validateSchema",
          "status" -> "error",
          "message" -> "Validation failed: #: 2 schema violations found, #: required key [destination] not found, #/chunks/size: expected type: Integer, found: String")))
    }.provide(prefilledStorage),

    test("should return proper info on missing schema") {
      for {
        res <- validateReq(SchemaId("another-schema"), "valid-json-test.json").flatMap(Boot.api)
        parsed <- Utils.extractJsonMap(res)
      } yield assert(res.status.code)(equalTo(404)) &&
        assert(parsed)(equalTo(Map(
          "id" -> "another-schema",
          "action" -> "validateSchema",
          "status" -> "error",
          "message" -> "Schema 'another-schema' not found in the storage")))
    }.provide(prefilledStorage),

  )
}
