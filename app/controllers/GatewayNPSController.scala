/*
 * Copyright 2021 HM Revenue & Customs
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
import models.PbikCredentials
import play.api.libs.json.Json
import play.api.mvc._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GatewayNPSController @Inject()(
  val tierConnector: HmrcTierConnectorWrapped,
  val configuration: PbikConfig,
  authenticate: MinimalAuthAction,
  val runModeConfiguration: Configuration,
  environment: Environment,
  val controllerUtils: ControllerUtils,
  cc: ControllerComponents)
    extends BackendController(cc) with play.api.Logging {

  val NO_HEADERS: Map[String, String] = Map[String, String]()

  /**
    * Method introduced in the middle tier ( PBIK ) to prevent Biks being registered during CY
    *
    * @param year The year for which the call is being made
    * @return Boolean true if either cy mode is enabled or if its disabled but the year supplied is not the current year
    **/
  def cyCheck(year: Int): Boolean =
    if (TaxYear.current.currentYear == year & !configuration.cyEnabled) {
      logger.warn(
        s"[GatewayNPSController][cyCheck] Support for Current Year is ${configuration.cyEnabled} and currentYear is ${TaxYear.current.currentYear} " +
          s"Attempt to update Benefits Type for Current Year rejected")
      false
    } else {
      true
    }

  def getRegisteredBenefits(empRef: String, year: Int): Action[AnyContent] = authenticate.async { implicit request =>
    controllerUtils.retrieveNPSCredentials(tierConnector, year, empRef) flatMap { credentials: PbikCredentials =>
      val url =
        s"${controllerUtils.baseURL}/$year/${credentials.payeSchemeType}/${credentials.employerNumber}/${credentials.payeSequenceNumber}"
      controllerUtils.generateResultBasedOnStatus(tierConnector.retrieveDataGet(url)(hc))
    }
  }

  def getExclusionsForEmployer(empRef: String, year: Int, ibdtype: Int): Action[AnyContent] = authenticate.async {
    implicit request =>
      controllerUtils.retrieveNPSCredentials(tierConnector, year, empRef) flatMap { credentials: PbikCredentials =>
        val url =
          s"${controllerUtils.baseURL}/$year/${credentials.payeSchemeType}/${credentials.employerNumber}/${credentials.payeSequenceNumber}/$ibdtype/${controllerUtils.exclusionPath}"
        controllerUtils.generateResultBasedOnStatus(tierConnector.retrieveDataGet(url)(hc))
      }
  }

  def updateBenefitTypes(empRef: String, year: Int): Action[AnyContent] = authenticate.async { implicit request =>
    if (cyCheck(year)) {
      controllerUtils.retrieveNPSCredentials(tierConnector, year, empRef) flatMap { credentials: PbikCredentials =>
        val url =
          s"${controllerUtils.baseURL}/$year/${credentials.payeSchemeType}/${credentials.employerNumber}/${credentials.payeSequenceNumber}/${controllerUtils.updateBenefitTypesPath}"
        controllerUtils.getNPSMutatorSessionHeader flatMap { res =>
          val headers = res.getOrElse(Map[String, String]())
          controllerUtils.generateResultBasedOnStatus(
            tierConnector
              .retrieveDataPost(headers, url, request.body.asJson.getOrElse(Json.toJson(List.empty[String])))(hc))
        }
      }
    } else {
      Future.successful(NotImplemented)
    }
  }

  def updateExclusionsForEmployer(empRef: String, year: Int, ibdtype: Int): Action[AnyContent] = authenticate.async {
    implicit request =>
      controllerUtils.retrieveNPSCredentials(tierConnector, year, empRef) flatMap { credentials: PbikCredentials =>
        val url =
          s"${controllerUtils.baseURL}/$year/${credentials.payeSchemeType}/${credentials.employerNumber}/${credentials.payeSequenceNumber}/$ibdtype/${controllerUtils.addExclusionPath}"
        controllerUtils.getNPSMutatorSessionHeader flatMap { res =>
          val headers = res.getOrElse(Map[String, String]())
          val response = controllerUtils.generateResultBasedOnStatus(
            tierConnector
              .retrieveDataPost(headers, url, request.body.asJson.getOrElse(Json.toJson(List.empty[String])))(hc))
          response.map { result =>
            result
          }
        }
      }

  }

  def removeExclusionForEmployer(empRef: String, year: Int, ibdtype: Int): Action[AnyContent] = authenticate.async {
    implicit request =>
      if (cyCheck(year)) {
        controllerUtils.retrieveNPSCredentials(tierConnector, year, empRef) flatMap { credentials: PbikCredentials =>
          val url =
            s"${controllerUtils.baseURL}/$year/${credentials.payeSchemeType}/${credentials.employerNumber}/${credentials.payeSequenceNumber}/$ibdtype/${controllerUtils.removeExclusionPath}"
          controllerUtils.getNPSMutatorSessionHeader flatMap { res =>
            val headers = res.getOrElse(Map[String, String]())
            controllerUtils.generateResultBasedOnStatus(
              tierConnector
                .retrieveDataPost(headers, url, request.body.asJson.getOrElse(Json.toJson(List.empty[String])))(hc))
          }
        }
      } else {
        Future.successful(NotImplemented)
      }
  }

}
