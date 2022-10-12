package thetask

import io.circe.ParsingFailure
import zio.test.Assertion._
import zio.test.{ZIOSpecDefault, _}

object SchemaManagementServiceSpec extends ZIOSpecDefault {

  lazy val spec = suite("SchemaManagementService")(
    test("return proper error on invalid json") {
      for {
        res <- JsonParser.parse(invalidJson).exit
      } yield assert(res)(fails(isSubtype[ParsingFailure](anything)))
    }
  )

  val invalidJson =
    """
      |{
      |  "$sema": "http://json-schema.org/draft-04/schema#",
      |  "type": "object",
      |  "properties": {
      |    "source": {
      |      "type": "string"
      |    },
      |    "destination": {
      |      "type": "string
      |    },
      |    "timeout": {
      |      "type": "integer",
      |      "minimum": 0,
      |      "maximum": 32767
      |    },
      |    "chunks": {
      |      "type": "object",
      |      "properties": {
      |        "size": {
      |          "type": "integer"
      |        },
      |        "number": {
      |          "type": "integer"
      |        }
      |      },
      |      "required": ["size"]
      |    }
      |  },
      |  "required": ["source", "destination"]
      |}
      |""".stripMargin
}