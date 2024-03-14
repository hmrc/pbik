/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import com.google.inject.Inject
import config.PbikConfig
import connectors.HmrcTierConnectorWrapped
import controllers.actions.MinimalAuthAction
import controllers.utils.ControllerUtils
import models.v1.{BenefitInKindRequest, BenefitListUpdateRequest, EmployerOptimisticLockRequest}
import models.{HeaderTags, PbikCredentials}
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

class GatewayNPSController @Inject() (
  val tierConnector: HmrcTierConnectorWrapped,
  val pbikConfig: PbikConfig,
  authenticate: MinimalAuthAction,
  val controllerUtils: ControllerUtils,
  cc: ControllerComponents
)(implicit val executionContext: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def getRegisteredBenefits(empRef: String, year: Int): Action[AnyContent] = authenticate.async { implicit request =>
    controllerUtils.retrieveNPSCredentials(tierConnector, year, empRef).flatMap { credentials: PbikCredentials =>
      controllerUtils.mapResponseToResult(tierConnector.getRegisteredBenefits(credentials, year)(hc))
    }
  }

  def getExclusionsForEmployer(empRef: String, year: Int, ibdtype: Int): Action[AnyContent] = authenticate.async {
    implicit request =>
      controllerUtils.retrieveNPSCredentials(tierConnector, year, empRef) flatMap { credentials: PbikCredentials =>
        val url =
          s"${pbikConfig.baseURL}/$year/${credentials.payeSchemeType}/${credentials.employerNumber}" +
            s"/${credentials.payeSequenceNumber}/$ibdtype/${pbikConfig.exclusionPath}"
        controllerUtils.generateResultBasedOnStatus(tierConnector.retrieveDataGet(url)(hc))
      }
  }

  def updateBenefitTypes(empRef: String, year: Int): Action[AnyContent] = authenticate.async { implicit request =>
    val biksToUpdate = request.body.asJson.flatMap(_.validate[List[BenefitInKindRequest]].asOpt).getOrElse(List.empty)
    controllerUtils.retrieveNPSCredentials(tierConnector, year, empRef) flatMap { credentials: PbikCredentials =>
      val url                = pbikConfig.putRegisteredBenefitsPath(credentials, year)
      val headers            = controllerUtils.getNPSMutatorSessionHeader
      val lockRequest        = EmployerOptimisticLockRequest(
        headers.getOrElse(HeaderTags.ETAG, HeaderTags.ETAG_DEFAULT_VALUE).toInt
      )
      val bikToUpdateRequest = BenefitListUpdateRequest(biksToUpdate, lockRequest)
      controllerUtils.mapResponseToResult(
        tierConnector.updateBenefitTypes(url, bikToUpdateRequest)(hc, request)
      )
    }
  }

  def updateExclusionsForEmployer(empRef: String, year: Int, ibdtype: Int): Action[AnyContent] = authenticate.async {
    implicit request =>
      controllerUtils.retrieveNPSCredentials(tierConnector, year, empRef) flatMap { credentials: PbikCredentials =>
        val url        =
          s"${pbikConfig.baseURL}/$year/${credentials.payeSchemeType}/${credentials.employerNumber}" +
            s"/${credentials.payeSequenceNumber}/$ibdtype/${pbikConfig.addExclusionPath}"
        val exclusions = request.body.asJson.getOrElse(Json.toJson(List.empty[String]))
        controllerUtils.generateResultBasedOnStatus(
          tierConnector.retrieveDataPost(url, exclusions)(hc, request)
        )
      }

  }

  def removeExclusionForEmployer(empRef: String, year: Int, ibdtype: Int): Action[AnyContent] = authenticate.async {
    implicit request =>
      controllerUtils.retrieveNPSCredentials(tierConnector, year, empRef) flatMap { credentials: PbikCredentials =>
        val url        =
          s"${pbikConfig.baseURL}/$year/${credentials.payeSchemeType}/${credentials.employerNumber}" +
            s"/${credentials.payeSequenceNumber}/$ibdtype/${pbikConfig.removeExclusionPath}"
        val exclusions = request.body.asJson.getOrElse(Json.toJson(List.empty[String]))
        controllerUtils.generateResultBasedOnStatus(
          tierConnector.retrieveDataPost(url, exclusions)(hc, request)
        )
      }
  }

}
