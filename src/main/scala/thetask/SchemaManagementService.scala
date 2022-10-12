package thetask

object SchemaManagementService {

  def store(data: JsonSchema) = {
    JsonParser.parse(data.value).mapError(InvalidJsonError(data.id, _, "json schema")) *>
      SchemasStorage.store(data).mapError(StorageError(data.id, _))
  }

  def retrieve(schemaId: SchemaId) =
    SchemasStorage.retrieve(schemaId)
      .mapError(StorageError(schemaId, _))
      .flatMap(_.toRight(SchemaNotFound(schemaId)).toIO)

}
