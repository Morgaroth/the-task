package thetask

import zio.{IO, Ref, ZIO, ZLayer}

case class SchemasStorageError(schemaId: SchemaId, detail: Throwable)

trait SchemasStorage {
  def get(schemaId: SchemaId): IO[SchemasStorageError, Option[JsonSchema]]

  def store(value: JsonSchema): IO[SchemasStorageError, Unit]
}

object SchemasStorage {
  def make(initData: Map[SchemaId, JsonSchema]): ZLayer[Any, Nothing, InMemSchemasStorage] =
    ZLayer.fromZIO(Ref.Synchronized.make(initData).map(new InMemSchemasStorage(_)))

  val inMem = make(Map.empty)

  def retrieve(schemaId: SchemaId): ZIO[SchemasStorage, SchemasStorageError, Option[JsonSchema]] = ZIO.service[SchemasStorage].flatMap(_.get(schemaId))

  def store(value: JsonSchema): ZIO[SchemasStorage, SchemasStorageError, Unit] = ZIO.service[SchemasStorage].flatMap(_.store(value))
}

class InMemSchemasStorage(
                           ref: Ref.Synchronized[Map[SchemaId, JsonSchema]]
                         ) extends SchemasStorage {
  override def get(schemaId: SchemaId): IO[SchemasStorageError, Option[JsonSchema]] = {
    ref.get.map(_.get(schemaId))
  }

  override def store(value: JsonSchema): IO[SchemasStorageError, Unit] = {
    ref.update(_.updated(value.id, value)).unit
  }
}
