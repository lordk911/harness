/*
 * Copyright ActionML, LLC under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * ActionML licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.actionml.authserver.services

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpEntity.Strict
import akka.http.scaladsl.model._
import akka.stream.Materializer
import akka.util.ByteString
import com.actionml.authserver._
import com.actionml.authserver.service.AuthorizationService
import com.actionml.circe.CirceSupport
import com.actionml.router.config.AppConfig
import io.circe.generic.auto._
import io.circe.syntax._
import scaldi.{Injectable, Injector}

import scala.concurrent.{ExecutionContext, Future}


trait AuthServerProxyService {
  def proxyAccessTokenRequest(request: HttpRequest): Future[HttpResponse]
}

class AuthServerClientService(implicit inj: Injector) extends AuthServerProxyService with AuthorizationService with CirceSupport with Injectable {
  private val config = inject[AppConfig]
  private implicit val ec = inject[ExecutionContext]
  private implicit val actorSystem = inject[ActorSystem]
  private implicit val materializer = inject[Materializer]

  override def proxyAccessTokenRequest(request: HttpRequest): Future[HttpResponse] = {
    val proxyRequest = mkAccessTokenRequest(request)
    Http().singleRequest(proxyRequest)
      .recoverWith {
        case ex =>
          Future.failed(AuthenticationFailedException(ex))
      }
  }

  override def authorize(accessToken: AccessToken, role: RoleId, resourceId: ResourceId): Future[Boolean] = {
    Http().singleRequest(mkAuthorizeRequest(accessToken, role, resourceId))
      .collect {
        case HttpResponse(StatusCodes.OK, _, _, _) => true
        case HttpResponse(_, _, _, _) => false
      }.recoverWith {
        case ex =>
          Future.successful(false)
      }
  }


  private val authServerRoot = Uri(config.auth.authServerUrl)

  private def mkAccessTokenRequest(req: HttpRequest) =
    HttpRequest(method = req.method,
      uri = authServerRoot.copy(path = authServerRoot.path + "/token"),
      entity = req.entity,
      headers = req.headers
    )

  private def mkAuthorizeRequest(accessToken: AccessToken, role: RoleId, resourceId: ResourceId) = {
    val body = Strict(ContentTypes.`application/json`, ByteString(AuthorizationCheckRequest(accessToken, role, resourceId).asJson.noSpaces))
    HttpRequest(method = HttpMethods.POST,
      uri = authServerRoot.copy(path = authServerRoot.path + "/authorize"),
      entity = body
    )
  }
}