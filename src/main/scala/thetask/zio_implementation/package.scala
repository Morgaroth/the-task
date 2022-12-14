package thetask

import zio.{IO, ZIO}

package object zio_implementation {

  implicit class Either2ZioPostfixSyntax[A, B](e: Either[A, B]) {
    def toIO: IO[A, B] = ZIO.fromEither(e)
  }
}
