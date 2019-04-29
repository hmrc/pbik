/*
 * Copyright 2019 HM Revenue & Customs
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
import config.RunModeConfig
import connectors.HmrcTierConnectorWrapped
import controllers.utils.ControllerUtils
import models.PbikCredentials
import play.api.libs.json.Json
import play.api.mvc._
import play.api.{Logger, Play}
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GatewayNPSController @Inject()(tierConnector:HmrcTierConnectorWrapped) extends BaseController with ControllerUtils with RunModeConfig{


  val NO_HEADERS = Map[String,String]()

  val cyEnabled: Boolean = Play.current.configuration.getBoolean("cymode.enabled").getOrElse(false)
  Logger.info("Current Year registrations allowed: " + cyEnabled)

  implicit val formats = Json.format[PbikCredentials]

  /**
   * Method introduced in the middle tier ( PBIK ) to prevent Biks being registered during CY
   * @param year The year for which the call is being made
   * @return Boolean true if either cy mode is enabled or if its disabled but the year supplied is not the current year
   * */
  def cyCheck(year: Int):Boolean = {
    if ( TaxYear.current.currentYear == year & cyEnabled == false) {
      Logger.warn("Support for Current Year is " + cyEnabled + " and currentYear is " +
        TaxYear.current.currentYear + ". Attempt to update Benefits Type for Current Year rejected")
      false
    } else {
      true
    }
  }

  def getRegisteredBenefits(empRef:String, year: Int) = Action.async {
    implicit request =>
      Logger.info("SESSIONID is " + hc.sessionId.getOrElse("No sessionId set"))
      val credentialsFuture = controllerUtils.retrieveNPSCredentials(tierConnector,year, empRef)
        credentialsFuture flatMap { credentials:PbikCredentials =>
        val url = s"$baseURL/$year/" + credentials.payeSchemeType + "/" + credentials.employerNumber + "/" + credentials.payeSequenceNumber
        controllerUtils.generateResultBasedOnStatus(tierConnector.retrieveDataGet(url)(hc))
      }
  }


  def getExclusionsForEmployer(empRef:String, year: Int, ibdtype: Int) = Action.async {
    implicit request =>
      val credentialsFuture = controllerUtils.retrieveNPSCredentials(tierConnector,year, empRef)
       credentialsFuture flatMap { credentials: PbikCredentials =>
        val url = s"$baseURL/$year/" + credentials.payeSchemeType + "/" + credentials.employerNumber + "/" + credentials.payeSequenceNumber + "/" + ibdtype + "/" + exclusionPath
        controllerUtils.generateResultBasedOnStatus(tierConnector.retrieveDataGet(url)(hc))
      }
  }

  def updateBenefitTypes(empRef:String, year: Int) = Action.async {
    implicit request =>
      if ( cyCheck(year) ) {
        val credentialsFuture = controllerUtils.retrieveNPSCredentials(tierConnector, year, empRef)
        credentialsFuture flatMap { credentials: PbikCredentials =>
          val url = s"$baseURL/$year/" + credentials.payeSchemeType + "/" + credentials.employerNumber + "/" + credentials.payeSequenceNumber + "/" + updateBenefitTypesPath
          controllerUtils.getNPSMutatorSessionHeader flatMap { res =>
            val headers = res.getOrElse(Map[String, String]())
            controllerUtils.generateResultBasedOnStatus(tierConnector.retrieveDataPost(headers, url, request.body.asJson.getOrElse(Json.toJson(List.empty[String])))(hc))
          }
        }
      } else {
        Future.successful(NotImplemented)
      }
  }

  def updateExclusionsForEmployer(empRef:String, year: Int, ibdtype: Int) = Action.async {
    implicit request =>
      val credentialsFuture = controllerUtils.retrieveNPSCredentials(tierConnector,year, empRef)
      credentialsFuture flatMap { credentials: PbikCredentials =>
        val url = s"$baseURL/$year/" + credentials.payeSchemeType + "/" + credentials.employerNumber + "/" + credentials.payeSequenceNumber + "/" + ibdtype + "/" + addExclusionPath
            controllerUtils.getNPSMutatorSessionHeader flatMap { res =>

            val headers = res.getOrElse(Map[String, String]())
            val response = controllerUtils.generateResultBasedOnStatus(tierConnector.retrieveDataPost(headers, url, request.body.asJson.getOrElse(Json.toJson(List.empty[String])))(hc))
            response.map{re =>
              re
            }
          }
      }

  }

  def removeExclusionForEmployer(empRef:String, year:Int, ibdtype:Int) = Action.async {
    implicit request =>
      if ( cyCheck(year) ) {
        val credentialsFuture = controllerUtils.retrieveNPSCredentials(tierConnector,year, empRef)
        credentialsFuture flatMap { credentials: PbikCredentials =>
          val url = s"$baseURL/$year/" + credentials.payeSchemeType + "/" + credentials.employerNumber + "/" + credentials.payeSequenceNumber + "/" + ibdtype + "/" + removeExclusionPath
          controllerUtils.getNPSMutatorSessionHeader flatMap { res =>
            val headers = res.getOrElse(Map[String, String]())
            controllerUtils.generateResultBasedOnStatus(tierConnector.retrieveDataPost(headers, url, request.body.asJson.getOrElse(Json.toJson(List.empty[String])))(hc))
          }
        }
      } else {
        Future.successful(NotImplemented)
      }
  }

}
