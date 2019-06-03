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

package helper

import connectors.HmrcTierConnectorWrapped
import controllers.utils.ControllerUtils
import javax.inject.Inject
import models.PbikCredentials
import play.api.libs.json
import play.api.mvc.{AnyContent, Request}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

// Stub this so we don't need to mock all the methods
  class StubbedControllerUtils @Inject()(environment: Environment,
                                         runModeConfiguration: Configuration) extends ControllerUtils(
                                                                                      environment,
                                                                                      runModeConfiguration)
{

    override def retrieveNPSCredentials(tierConnector: HmrcTierConnectorWrapped,year: Int, empRef:String)
                              (implicit request: Request[AnyContent], hc: HeaderCarrier, formats: json.Format[PbikCredentials]):
    Future[PbikCredentials] = Future.successful( new PbikCredentials(0,0,0,"","") )

    override def getNPSMutatorSessionHeader(implicit request: Request[AnyContent], hc: HeaderCarrier):
          Future[Option[Map[String, String]]] = Future.successful(Some(Map.empty[String,String]))
  }
