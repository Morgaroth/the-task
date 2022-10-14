package thetask
package zio_implementation

import io.circe.{Json, ParsingFailure}
import zio.IO


object JsonParser {

  def parse(input: String): IO[ParsingFailure, Json] = {
    io.circe.parser.parse(input).toIO
  }
}
