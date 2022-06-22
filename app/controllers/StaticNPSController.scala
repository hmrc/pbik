/*
 * Copyright 2022 HM Revenue & Customs
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
import connectors.HmrcTierConnectorWrapped
import controllers.actions.MinimalAuthAction
import controllers.utils.ControllerUtils
import play.api.Configuration
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

class StaticNPSController @Inject()(
  val tierConnector: HmrcTierConnectorWrapped,
  val runModeConfiguration: Configuration,
  authenticate: MinimalAuthAction,
  controllerUtils: ControllerUtils,
  cc: ControllerComponents)
    extends BackendController(cc) {

  def getBenefitTypes(year: Int): Action[AnyContent] = authenticate.async { implicit request =>
    val url = s"${controllerUtils.baseURL}/$year/${controllerUtils.getBenefitTypesPath}"
    controllerUtils.generateResultBasedOnStatus(tierConnector.retrieveDataGet(url)(hc))
  }

}
