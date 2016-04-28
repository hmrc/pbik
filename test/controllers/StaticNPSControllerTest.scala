/*
 * Copyright 2016 HM Revenue & Customs
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
import controllers.utils.ControllerUtilsWrapped
import org.scalatest.mock.MockitoSugar
import org.specs2.mock.mockito.MockitoMatchers
import play.api.libs.json
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeApplication
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.HttpResponse
import uk.gov.hmrc.play.test.UnitSpec
import org.mockito.Mockito._
import scala.concurrent.Future

class StaticNPSControllerTest extends UnitSpec with MockitoSugar with MockitoMatchers with FakePBIKApplication {

  class FakeResponse extends HttpResponse {
    override val allHeaders = Map[scala.Predef.String, scala.Seq[scala.Predef.String]]()
    override def status = 200
    override val json = Json.parse(sampleBikJson)
    override val body = sampleBikJson

  }

  trait StubServicesConfig extends ServicesConfig {
    override def baseUrl(serviceName:String) = "https:9000"
  }

  // Stub this so we don't need to mock all the methods
  class StubbedControllerWrapped extends ControllerUtilsWrapped {

  }

  val staticNPSController = new StaticNPSController with StubServicesConfig {
    override val controllerUtils = new StubbedControllerWrapped
    override val tierConnector = mock[HmrcTierConnectorWrapped]


    when(tierConnector.retrieveDataGet(anyString)(any[HeaderCarrier])).thenReturn(Future.successful(new FakeResponse))
  }

  "When getting Benefits Types the Controller " should {
    " parse a response correctly and not mutate the returned response body " in {
      running( new FakeApplication() ) {
        val CY = 2015
        val result = await(staticNPSController.getBenefitTypes(CY).apply(mockrequest))
        status(result) shouldBe(200)
        bodyOf(result) shouldBe(sampleBikJson)
      }
    }
  }

}
