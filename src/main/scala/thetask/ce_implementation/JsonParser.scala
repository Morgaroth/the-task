package thetask.ce_implementation

import cats.Applicative
import cats.data.EitherT
import io.circe.{Json, ParsingFailure}


object JsonParser {

  def parse[F[_] : Applicative](input: String): EitherT[F, ParsingFailure, Json] = {
    EitherT.fromEither[F](io.circe.parser.parse(input))
  }
}
