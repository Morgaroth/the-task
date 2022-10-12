package thetask

import cats.data.NonEmptyList
import io.circe.ParsingFailure
import io.circe.schema.ValidationError

trait ThisTaskErrorDomain extends Product {
  def toHttpError(action: Action): ThisTaskHttpError
}

case class SchemaNotFound(schemaId: SchemaId) extends ThisTaskErrorDomain {
  override def toHttpError(action: Action) = NotFoundError(schemaId, action, Status.Error, s"Schema '${schemaId.value}' not found in the storage")
}

case class StorageError(schemaId: SchemaId, error: SchemasStorageError) extends ThisTaskErrorDomain {
  override def toHttpError(action: Action) =
    error match {
      case SchemaAlreadyExists(_) => BadRequestError(schemaId, action, Status.Error, s"Schema '${schemaId.value}' already exists")
      case error => InternalServerError(schemaId, action, Status.Error, s"An internal error happened $error")
    }
}

case class InvalidJsonError(schemaId: SchemaId, p: ParsingFailure, detail: String) extends ThisTaskErrorDomain {
  override def toHttpError(action: Action) =
    BadRequestError(schemaId, action, Status.Error, s"Provided json for $detail cannot be parsed: $p")
}

case class JsonValidationAgainstSchemaError(schemaId: SchemaId, validationErrors: NonEmptyList[ValidationError]) extends ThisTaskErrorDomain {
  override def toHttpError(action: Action) =
    BadRequestError(schemaId, action, Status.Error, s"Validation failed: ${validationErrors.map(_.getMessage).toList.mkString(", ")}")
}
