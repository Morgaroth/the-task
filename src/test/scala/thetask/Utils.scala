package thetask

import io.circe.jawn
import zhttp.http.Response
import zio.ZIO

object Utils {

  def extractJsonMap(response: Response) = {
    for {
      bodyStr <- response.bodyAsString
      parsed <- ZIO.fromEither(jawn.decode[Map[String, String]](bodyStr)).mapError(x => new RuntimeException(s"bad json $x"))
    } yield parsed

  }
}
