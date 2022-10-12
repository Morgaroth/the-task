package thetask

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

trait ThisTaskHttpError extends Product with Serializable

case class BadRequestError(id: SchemaId, action: Action, status: Status, message: String) extends ThisTaskHttpError

object BadRequestError {
  implicit val circeCodec: Codec[BadRequestError] = deriveCodec
}

case class NotFoundError(id: SchemaId, action: Action, status: Status, message: String) extends ThisTaskHttpError

object NotFoundError {
  implicit val circeCodec: Codec[NotFoundError] = deriveCodec
}

case class InternalServerError(id: SchemaId, action: Action, status: Status, message: String) extends ThisTaskHttpError

object InternalServerError {
  implicit val circeCodec: Codec[InternalServerError] = deriveCodec
}

