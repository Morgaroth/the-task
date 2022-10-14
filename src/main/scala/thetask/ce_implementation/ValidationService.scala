package thetask
package ce_implementation

import cats.Monad
import cats.data.EitherT
import io.circe.schema.Schema
import io.circe.{Json, JsonObject}


class ValidationService[F[_] : Monad](retrievalService: SchemaRetrievalService[F]) {

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

  def validate(schemaId: SchemaId, data: String) = {
    (for {
      schema <- EitherT(retrievalService.retrieve(schemaId))
      parsedInput <- JsonParser.parse(data).leftMap[ThisTaskErrorDomain](InvalidJsonError(schemaId, _, "input json")).map(cleanJsonInputWithOwn)
      parsedSchema <- JsonParser.parse(schema.value).leftMap[ThisTaskErrorDomain](InvalidJsonError(schemaId, _, "json schema"))
      res <- EitherT.fromEither(Schema.load(parsedSchema).validate(parsedInput).toEither).leftMap[ThisTaskErrorDomain](JsonValidationAgainstSchemaError(schemaId, _))
    } yield res
      ).value
  }
}
