package thetask

import io.circe.schema.Schema
import io.circe.{Json, JsonObject}
import zio.ZIO


object ValidationService {

  def cleanJsonInputWithLib(data: Json): Json = {
    data.deepDropNullValues // but ok, will implement it on my own, if you want to see some logic :)

  }

  def cleanJsonInputWithOwn(data: Json): Json = {
    data
      .mapArray(arr => arr.filterNot(_.isNull).map(cleanJsonInputWithOwn)) // do remove nulls from Array?
      .mapObject { obj =>
        val elements = obj.toList.collect { case (k, v) if !v.isNull =>
          k -> cleanJsonInputWithOwn(v)
        }.toMap
        JsonObject.fromMap(elements)
      }
    // do nothing if anything else
  }

  def validate(schemaId: SchemaId, data: String): ZIO[SchemasStorage, ThisTaskErrorDomain, Unit] = {
    for {
      schema <- SchemasStorage.retrieve(schemaId)
        .mapError(StorageError(schemaId, _)).flatMap(_.toRight(SchemaNotFound(schemaId)).toIO)
      parsedInput <- JsonParser.parse(data).mapError(InvalidJsonError(schemaId, _, "input json")).map(cleanJsonInputWithOwn)
      parsedSchema <- JsonParser.parse(schema.value).mapError(InvalidJsonError(schemaId, _, "json schema"))
      res <- Schema.load(parsedSchema).validate(parsedInput).toEither.toIO.mapError(JsonValidationAgainstSchemaError(schemaId, _))
    } yield res
  }
}
