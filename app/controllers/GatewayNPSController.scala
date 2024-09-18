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
import connectors.NpsConnector
import controllers.actions.MinimalAuthAction
import models.v1
import play.api.Logging
import play.api.libs.json.JsObject
import play.api.mvc._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class GatewayNPSController @Inject() (
  npsConnector: NpsConnector,
  authenticate: MinimalAuthAction,
  cc: ControllerComponents
)(implicit val ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  /** Maps an HttpResponse to a Play Result Because HttpResponse from play and Result from play have different ways of
    * representing headers, had to write this custom mapping logic to convert the headers from Map[String,
    * Seq[String\]\] to Seq[(String, String)]
    * @param httpResponse
    *   - Response from NPS call in NpsConnector
    * @return
    *   play.api.mvc.Result
    */
  private def mapHttpResponseToResult(httpResponse: HttpResponse): Result = {
    val status  = httpResponse.status
    val body    = httpResponse.body
    val headers = httpResponse.headers
      .flatMap { case (key, values) => values.map(value => (key, value)) }
      .toSeq
      .sortBy(_._1)
    Status(status)(body).withHeaders(headers: _*)
  }

  def getBenefitTypes(year: Int): Action[AnyContent] = authenticate.async { implicit request =>
    npsConnector.getBenefitTypes(year).map(mapHttpResponseToResult)
  }

  def getRegisteredBenefits(taxOfficeNumber: String, taxOfficeReference: String, year: Int): Action[AnyContent] =
    authenticate.async { implicit request =>
      npsConnector.getPbikCredentials(taxOfficeNumber, taxOfficeReference, year).flatMap {
        credentials: v1.PbikCredentials =>
          npsConnector
            .getRegisteredBenefits(credentials, year)
            .map(mapHttpResponseToResult)
      }
    }

  def getExclusionsForEmployer(
    taxOfficeNumber: String,
    taxOfficeReference: String,
    year: Int,
    iabd: String
  ): Action[AnyContent] = authenticate.async { implicit request =>
    npsConnector.getPbikCredentials(taxOfficeNumber, taxOfficeReference, year).flatMap {
      credentials: v1.PbikCredentials =>
        npsConnector
          .getAllExcludedPeopleForABenefit(credentials, year, iabd)
          .map(mapHttpResponseToResult)
    }
  }

  def updateBenefitTypes(taxOfficeNumber: String, taxOfficeReference: String, year: Int): Action[AnyContent] =
    authenticate.async { implicit request =>
      val biksToUpdate = request.body.asJson.getOrElse(JsObject.empty)
      npsConnector.getPbikCredentials(taxOfficeNumber, taxOfficeReference, year).flatMap {
        credentials: v1.PbikCredentials =>
          npsConnector
            .updateBenefitTypes(credentials, year, biksToUpdate)
            .map(mapHttpResponseToResult)
      }
    }

  def updateExclusionsForEmployer(taxOfficeNumber: String, taxOfficeReference: String, year: Int): Action[AnyContent] =
    authenticate.async { implicit request =>
      val exclusions = request.body.asJson.getOrElse(JsObject.empty)
      npsConnector.getPbikCredentials(taxOfficeNumber, taxOfficeReference, year).flatMap {
        credentials: v1.PbikCredentials =>
          npsConnector
            .updateExcludedPeopleForABenefit(credentials, year, exclusions)
            .map(mapHttpResponseToResult)
      }

    }

  def removeExclusionForEmployer(
    taxOfficeNumber: String,
    taxOfficeReference: String,
    year: Int,
    iabd: String
  ): Action[AnyContent] =
    authenticate.async { implicit request =>
      val exclusions = request.body.asJson.getOrElse(JsObject.empty)
      npsConnector.getPbikCredentials(taxOfficeNumber, taxOfficeReference, year).flatMap {
        credentials: v1.PbikCredentials =>
          npsConnector
            .removeExcludedPeopleForABenefit(credentials, year, iabd, exclusions)
            .map(mapHttpResponseToResult)
      }
    }

  def tracePeopleByPersonalDetails(taxOfficeNumber: String, taxOfficeReference: String, year: Int): Action[AnyContent] =
    authenticate.async { implicit request =>
      val exclusions = request.body.asJson.getOrElse(JsObject.empty)
      npsConnector.getPbikCredentials(taxOfficeNumber, taxOfficeReference, year).flatMap {
        credentials: v1.PbikCredentials =>
          npsConnector
            .tracePeopleByPersonalDetails(credentials, year, exclusions)
            .map(mapHttpResponseToResult)
      }
    }

}
