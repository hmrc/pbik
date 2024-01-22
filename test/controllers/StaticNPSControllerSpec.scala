/*
 * Copyright 2023 HM Revenue & Customs
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

import connectors.HmrcTierConnectorWrapped
import controllers.actions.MinimalAuthAction
import controllers.utils.ControllerUtils
import helper.{StubbedControllerUtils, TestMinimalAuthAction}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

class StaticNPSControllerSpec extends PlaySpec with MockitoSugar with FakePBIKApplication {

  implicit lazy override val app: Application = new GuiceApplicationBuilder()
    .configure(config)
    .overrides(bind(classOf[HmrcTierConnectorWrapped]).toInstance(mock[HmrcTierConnectorWrapped]))
    .overrides(bind(classOf[ControllerUtils]).to(classOf[StubbedControllerUtils]))
    .overrides(bind(classOf[MinimalAuthAction]).to(classOf[TestMinimalAuthAction]))
    .build()

  class FakeResponse extends HttpResponse {
    override val allHeaders: Map[String, Seq[String]] = Map[scala.Predef.String, scala.Seq[scala.Predef.String]]()
    override def status                               = 200
    override val json: JsValue                        = Json.parse(sampleBikJson)
    override val body: String                         = sampleBikJson
  }

  val staticNPSController: StaticNPSController = {
    val snc = app.injector.instanceOf[StaticNPSController]
    when(snc.tierConnector.retrieveDataGet(anyString)(any[HeaderCarrier]))
      .thenReturn(Future.successful(new FakeResponse))
    snc
  }

  "When getting Benefits Types the Controller " should {
    " parse a response correctly and not mutate the returned response body " in {
      val CY     = 2015
      val result = staticNPSController.getBenefitTypes(CY)(mockrequest)
      status(result)          must be(OK)
      contentAsString(result) must be(sampleBikJson)
    }
  }

}
