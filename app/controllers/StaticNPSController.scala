/*
 * Copyright 2015 HM Revenue & Customs
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

import uk.gov.hmrc.play.microservice.controller.BaseController
import controllers.utils.ControllerUtils
import connectors.HmrcTierConnector
import play.api.libs.json.Json
import models.PbikCredentials
import play.api.mvc.Action

object StaticNPSController extends StaticNPSController

class StaticNPSController extends BaseController with ControllerUtils with HmrcTierConnector {

  implicit val formats = Json.format[PbikCredentials]

  def getBenefitTypes(year: Int) = Action.async {
    implicit request =>
      val url = s"$baseURL/$year/$getBenefitTypesPath"
      controllerUtils.generateResultBasedOnStatus(tierConnector.retrieveDataGet(url)(hc))
  }

}
