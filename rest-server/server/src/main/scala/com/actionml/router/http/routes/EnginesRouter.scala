package com.actionml.router.http.routes

import akka.actor.ActorRef
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.SecurityDirectives
import akka.pattern.ask
import com.actionml.admin.Administrator
import com.actionml.authserver.ResourceId
import com.actionml.authserver.Roles.engine
import com.actionml.authserver.directives.AuthorizationDirectives
import com.actionml.authserver.service.AuthorizationService
import com.actionml.authserver.services.AuthServerProxyService
import com.actionml.router.config.{AppConfig, ConfigurationComponent}
import com.actionml.router.service._
import io.circe.Json
import scaldi.Injector

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  *
  * Engine endpoints:
  *
  * Add new engine
  * PUT, POST /engines/ {JSON body for PIO engine}
  * Response: HTTP code 201 if the engine was successfully created; otherwise, 400.
  *
  * Todo: Update existing engine
  * PUT, POST /engines/<engine-id>?data_delete=true&force=true {JSON body for PIO event}
  * Response: HTTP code 200 if the engine was successfully updated; otherwise, 400.
  *
  * Update existing engine
  * PUT, POST /engines/<engine-id>?import=true&location=String
  * Response: HTTP code 200 if the engine was successfully updated; otherwise, 400.
  *
  * Get existing engine
  * GET /engines/<engine-id>
  * Response: {JSON body for PIO event}
  * HTTP code 200 if the engine exist; otherwise, 404
  *
  * Delete existing engine
  * DELETE /engines/<engine-id>
  * Response: HTTP code 200 if the engine was successfully deleted; otherwise, 400.
  *
  * @author The ActionML Team (<a href="http://actionml.com">http://actionml.com</a>)
  * 29.01.17 17:36
  */
class EnginesRouter(implicit inj: Injector) extends BaseRouter with AuthorizationDirectives {
  private val engineService = inject[ActorRef]('EngineService)
  override val authorizationService = inject[AuthorizationService]
  private val config = inject[AppConfig]
  override val authEnabled = config.auth.enabled

  override val route: Route = (rejectEmptyResponse & extractAccessToken) { implicit accessToken =>
    (pathPrefix("engines") & extractLog) { implicit log =>
      (pathEndOrSingleSlash & hasAccess(engine.create, ResourceId.*)) {
        getEngines ~
        createEngine
      } ~
      pathPrefix(Segment) { engineId ⇒
        hasAccess(engine.read, engineId).apply {
          getEngine(engineId)
        } ~
        hasAccess(engine.modify, engineId).apply {
          delete(deleteEngine(engineId)) ~
          (post & path("imports"))(updateEngineWithImport(engineId)) ~
          (post & path("configs"))(updateEngineWithConfig(engineId))
        }
      }
    }
  }


  private def getEngine(engineId: String)(implicit log: LoggingAdapter): Route = get {
    log.info("Get engine: {}", engineId)
    completeByValidated(StatusCodes.OK) {
      (engineService ? GetEngine(engineId)).mapTo[Response]
    }
  }

  private def getEngines(implicit log: LoggingAdapter): Route = get {
    log.info("Get engines information")
    completeByValidated(StatusCodes.OK) {
      (engineService ? GetEngines()).mapTo[Response]
    }
  }

  private def createEngine(implicit log: LoggingAdapter): Route = asJson { engineConfig =>
    log.info("Create engine: {}", engineConfig)
    completeByValidated(StatusCodes.Created) {
      (engineService.ask(CreateEngine(engineConfig.toString()))(5.seconds)).mapTo[Response]
    }
  }

  private def updateEngineWithConfig(engineId: String)(implicit log: LoggingAdapter): Route = entity(as[Json]) { engineConfig ⇒
    log.info("Update engine: {}, updateConfig: true", engineId)
    completeByValidated(StatusCodes.OK) {
      (engineService ? UpdateEngine( engineConfig.toString()) ).mapTo[Response]
    }
  }

  private def updateEngineWithImport(engineId: String)(implicit log: LoggingAdapter): Route = parameter('import_path) { importPath ⇒
    log.info("Update engine: {}, importPath: {}", engineId, importPath)
    completeByValidated(StatusCodes.OK) {
      (engineService ? UpdateEngineWithImport(engineId, importPath)).mapTo[Response]
    }
  }

  private def deleteEngine(engineId: String)(implicit log: LoggingAdapter): Route = {
    log.info("Delete engine: {}", engineId)
    completeByValidated(StatusCodes.OK) {
      (engineService.ask(DeleteEngine(engineId))(5.seconds)).mapTo[Response]
    }
  }
}
