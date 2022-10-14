package thetask
package ce_implementation

import cats.data.EitherT
import cats.syntax.either._
import cats.syntax.functor._
import cats.{Applicative, Monad}

class SchemaRetrievalService[F[_] : Applicative](storage: SchemasStorage[F]) {
  def retrieve(schemaId: SchemaId) = {
    storage.get(schemaId).map {
      _.leftMap[ThisTaskErrorDomain](StorageError(schemaId, _))
        .flatMap(_.toRight[ThisTaskErrorDomain](SchemaNotFound(schemaId)))
    }
  }
}

class SchemaStoreService[F[_] : Monad](storage: SchemasStorage[F]) {

  def store(data: JsonSchema) = {
    JsonParser.parse(data.value)
      .leftMap[ThisTaskErrorDomain](InvalidJsonError(data.id, _, "json schema"))
      .flatMap(_ =>
        EitherT(storage.store(data)).leftMap[ThisTaskErrorDomain](x => StorageError(data.id, x))
      ).value
  }
}
