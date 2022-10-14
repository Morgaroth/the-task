package thetask

import cats.syntax.either._

import java.math.BigInteger
import java.security.MessageDigest

trait SchemasStorageError

case class SchemaAlreadyExists(schemaId: SchemaId) extends SchemasStorageError

case class CantReadSchemaFromStorage(schemaId: SchemaId, error: String) extends SchemasStorageError

case class CantStoreSchema(schemaId: SchemaId, error: String) extends SchemasStorageError


object SchemaIdToMd5 {
  def hashSchemaID(schemaId: SchemaId) = Either.catchNonFatal {
    val hasher = MessageDigest.getInstance("MD5")
    hasher.update(schemaId.value.getBytes)
    val bigInt = new BigInteger(1, hasher.digest())
    bigInt.toString(16).toUpperCase
  }
}
