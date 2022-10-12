package thetask

import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Decoder, Encoder}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

import scala.io.Source

case class JsonSchema(
                       id: SchemaId,
                       value: String,
                     )

case class SchemaId(value: String) extends AnyVal

object SchemaId {
  val pathParam = path[SchemaId].name("schemaId").example(SchemaId("config-schema"))
  implicit val tapirSchema: Schema[SchemaId] = Schema.string
  implicit val circeEncoder: Encoder[SchemaId] = Encoder.encodeString.contramap[SchemaId](_.value)
  implicit val circeDecoder: Decoder[SchemaId] = Decoder.decodeString.map(SchemaId.apply)
}

object endpoints {

  val schemaRegister: Endpoint[Unit, JsonSchema, ThisTaskHttpError, GenericSuccess, Any] =
    endpoint
      .post
      .in("schema" / SchemaId.pathParam) // kinda not typical CRUD
      .in(stringJsonBody.example(Source.fromResource("config-schema.json").mkString))
      .mapInTo[JsonSchema]
      .out(statusCode(StatusCode.Created).and(jsonBody[GenericSuccess]))
      .errorOut(
        oneOf(
          oneOfVariant(StatusCode.BadRequest, jsonBody[BadRequestError]),
          oneOfVariant(StatusCode.InternalServerError, jsonBody[InternalServerError]),
        ))

  val schemaGet: Endpoint[Unit, SchemaId, ThisTaskHttpError, String, Any] =
    endpoint
      .get
      .in("schema" / SchemaId.pathParam)
      .out(statusCode(StatusCode.Ok).and(stringJsonBody))
      .errorOut(oneOf(
        oneOfVariant(StatusCode.NotFound, jsonBody[NotFoundError]),
        oneOfVariant(StatusCode.InternalServerError, jsonBody[InternalServerError]),
      ))

  val jsonValidate =
    endpoint
      .post
      .in("validate" / SchemaId.pathParam)
      .in(stringJsonBody.example(Source.fromResource("valid-json-test.json").mkString))
      .out(statusCode(StatusCode.Ok).and(jsonBody[GenericSuccess]))
      .errorOut(oneOf(
        oneOfVariant(StatusCode.BadRequest, jsonBody[BadRequestError]),
        oneOfVariant(StatusCode.NotFound, jsonBody[NotFoundError]),
        oneOfVariant(StatusCode.InternalServerError, jsonBody[InternalServerError]),
      ))
}

sealed abstract class Status(val name: String) extends Product

object Status {
  case object Success extends Status("success")

  case object Error extends Status("error")

  val allValues = Vector(Success, Error).map(x => x.name.toLowerCase -> x).toMap
  implicit val circeEncoder: Encoder[Status] = Encoder.encodeString.contramap[Status](_.name)
  implicit val circeDecoder: Decoder[Status] = Decoder.decodeString.emap(name => allValues.get(name.toLowerCase).toRight(s"value $name unknown for Status class"))
  implicit val tapirSchema: Schema[Status] = Schema.string
}

sealed abstract class Action(val name: String) extends Product

object Action {
  case object GetSchema extends Action("getSchema")

  case object UploadSchema extends Action("uploadSchema")

  case object ValidateSchema extends Action("validateSchema")

  val allValues = Vector(GetSchema, UploadSchema, ValidateSchema).map(x => x.name.toLowerCase -> x).toMap
  implicit val circeEncoder: Encoder[Action] = Encoder.encodeString.contramap[Action](_.name)
  implicit val circeDecoder: Decoder[Action] = Decoder.decodeString.emap(name => allValues.get(name.toLowerCase).toRight(s"value $name unknown for Action class"))
  implicit val tapirSchema: Schema[Action] = Schema.string
}

case class GenericSuccess(id: SchemaId, action: Action, status: Status)

object GenericSuccess {
  implicit val GenericSuccessCodec: Codec[GenericSuccess] = deriveCodec
}
