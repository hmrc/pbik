/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers.utils

import uk.gov.hmrc.play.config.ServicesConfig

trait URIInformation extends ServicesConfig {

  val getBenefitTypesPath = "getbenefittypes"
  val updateBenefitTypesPath = "update"
  val exclusionPath = "exclusion"
  val addExclusionPath = "exclusion/update"
  val removeExclusionPath ="exclusion/remove"
  // serviceUrl - the base URL, on which the service status call is hosted
  // baseUrl - the base URL, on which the service status call is hosted
  lazy val serviceUrl: String = baseUrl("nps") + "/nps-hod-service/services/nps"
  lazy val baseURL: String = System.getProperty("OverridePbikUrl", baseUrl("nps") + "/nps-hod-service/services/nps") + "/employer/payroll-bik"
  val statusPath = "ServiceStatus"

}
