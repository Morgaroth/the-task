package thetask
package zio_implementation

import better.files.Dsl
import sttp.capabilities.zio.ZioStreams
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir._
import zhttp.service.Server
import zio.Task

object Boot extends zio.ZIOAppDefault {

  val storage = SchemasStorage.makeFileBased(Dsl.cwd / "schemas-storage") // configurability out of scope

  val schemasRetrievalServerEndpoint: ZServerEndpoint[SchemasStorage, Any] =
    endpoints
      .schemaGet
      .zServerLogic { schemaId =>
        SchemaManagementService.retrieve(schemaId)
          .map(_.value)
          .mapError(_.toHttpError(Action.GetSchema))
      }

  val schemasUploadServerEndpoint: ZServerEndpoint[SchemasStorage, Any] =
    endpoints
      .schemaRegister
      .zServerLogic { data =>
        SchemaManagementService.store(data)
          .as(GenericSuccess(data.id, Action.UploadSchema, Status.Success))
          .mapError(_.toHttpError(Action.UploadSchema))
      }

  val jsonValidateServerEndpoint: ZServerEndpoint[SchemasStorage, Any] =
    endpoints
      .jsonValidate
      .zServerLogic { case (schemaId, jsonValue) =>
        ValidationService.validate(schemaId, jsonValue)
          .as(GenericSuccess(schemaId, Action.ValidateDocument, Status.Success))
          .mapError(_.toHttpError(Action.ValidateDocument))
      }

  val serviceEndpoints: List[ZServerEndpoint[SchemasStorage, ZioStreams]] = List(
    schemasUploadServerEndpoint,
    schemasRetrievalServerEndpoint,
    jsonValidateServerEndpoint,
  )

  val swaggerUIEndpoints = SwaggerInterpreter().fromEndpoints[Task](serviceEndpoints.map(_.endpoint), "Mateusz's Recruitment Task", "1.0")
    .map(_.widen[SchemasStorage])

  val api = ZioHttpInterpreter().toHttp(serviceEndpoints ++ swaggerUIEndpoints)

  override val run = Server.start(8080, api).exitCode.provideLayer(storage)
}
