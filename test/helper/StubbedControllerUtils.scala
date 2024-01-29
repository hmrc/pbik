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

package helper

import config.PbikConfig
import connectors.HmrcTierConnectorWrapped
import controllers.utils.ControllerUtils
import models.PbikCredentials
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StubbedControllerUtils @Inject() (pbikConfig: PbikConfig) extends ControllerUtils(pbikConfig) {

  override def retrieveNPSCredentials(tierConnector: HmrcTierConnectorWrapped, year: Int, empRef: String)(implicit
    hc: HeaderCarrier
  ): Future[PbikCredentials] =
    Future.successful(new PbikCredentials(0, 0, 0, "", ""))

  override def getNPSMutatorSessionHeader(implicit request: Request[_]): Map[String, String] =
    Map.empty[String, String]
}
