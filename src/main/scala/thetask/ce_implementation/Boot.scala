package thetask
package ce_implementation

import better.files.Dsl
import cats.effect.{ExitCode, IO}
import cats.syntax.either._
import org.http4s.blaze.server.BlazeServerBuilder
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object Boot extends cats.effect.IOApp {

  val storage = new FileBasedStorage[IO](Dsl.cwd / "schemas-storage") // configurability out of scope
  val retrievalService = new SchemaRetrievalService[IO](storage)
  val uploadService = new SchemaStoreService[IO](storage)
  val validationService = new ValidationService[IO](retrievalService)


  val schemasRetrievalServerEndpoint =
    endpoints
      .schemaGet
      .serverLogic { schemaId =>
        retrievalService.retrieve(schemaId)
          .map(_.bimap(_.toHttpError(Action.GetSchema), _.value))
      }

  val schemasUploadServerEndpoint =
    endpoints
      .schemaRegister
      .serverLogic { data =>
        uploadService.store(data).map {
          _.bimap(_.toHttpError(Action.UploadSchema), _ => GenericSuccess(data.id, Action.UploadSchema, Status.Success))
        }
      }

  val jsonValidateServerEndpoint =
    endpoints
      .jsonValidate
      .serverLogic { case (schemaId, jsonValue) =>
        validationService.validate(schemaId, jsonValue)
          .map {
            _.bimap(_.toHttpError(Action.ValidateDocument), _ => GenericSuccess(schemaId, Action.ValidateDocument, Status.Success))
          }
      }

  val serviceEndpoints = List(
    schemasUploadServerEndpoint,
    schemasRetrievalServerEndpoint,
    jsonValidateServerEndpoint,
  )

  val swaggerUIEndpoints: List[ServerEndpoint[Any, IO]] = SwaggerInterpreter().fromEndpoints[IO](serviceEndpoints.map(_.endpoint), "Mateusz's Recruitment Task", "1.0")

  val api = Http4sServerInterpreter[IO]().toRoutes(serviceEndpoints ++ swaggerUIEndpoints).orNotFound

  def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withHttpApp(api)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
}
