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

import connectors.HmrcTierConnectorWrapped
import controllers.actions.MinimalAuthAction
import controllers.utils.ControllerUtils
import helper.{StubbedControllerUtils, TestMinimalAuthAction}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.http.HttpEntity._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.{await, _}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

class GatewayNPSControllerSpec extends PlaySpec with MockitoSugar with FakePBIKApplication {

  implicit lazy override val app: Application = new GuiceApplicationBuilder()
    .configure(config)
    .overrides(bind(classOf[HmrcTierConnectorWrapped]).toInstance(mock[HmrcTierConnectorWrapped]))
    .overrides(bind(classOf[ControllerUtils]).to(classOf[StubbedControllerUtils]))
    .overrides(bind(classOf[MinimalAuthAction]).to(classOf[TestMinimalAuthAction]))
    .build()

  class FakeResponse extends HttpResponse {
    override val allHeaders: Map[String, Seq[String]] = Map[scala.Predef.String, scala.Seq[scala.Predef.String]]()

    override def status = 200

    override val json: JsValue = Json.parse(sampleBikJson)
    override val body: String = sampleBikJson
  }

  val StubbedGateway: GatewayNPSController = {

    val gnc = app.injector.instanceOf[GatewayNPSController]

    when(gnc.tierConnector.retrieveDataGet(anyString)(any[HeaderCarrier]))
      .thenReturn(Future.successful(new FakeResponse))
    when(gnc.tierConnector.retrieveDataPost(any[Map[String, String]], anyString, any[JsValue])(any[HeaderCarrier]))
      .thenReturn(Future.successful(new FakeResponse))

    gnc
  }

  "When getting Benefits Types the Controller " should {
    " parse a response correctly and not mutate the returned response body " in {
      val gateway = StubbedGateway
      val CY = 2015
      val result = await(gateway.getRegisteredBenefits("123/TEST1", 2015).apply(mockrequest))
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must be(sampleBikJson)
    }
  }

  "When getting exclusions the Controller " should {
    " parse a response correctly and not mutate the returned response body " in {
      val gateway = StubbedGateway
      val CY = 2015
      val result = await(gateway.getExclusionsForEmployer("123/TEST1", 2015, 37).apply(mockrequest))
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must be(sampleBikJson)
    }
  }

  "When updating exclusions the Controller " should {
    " parse a response correctly and not mutate the returned response body - update " in {
      val gateway = StubbedGateway
      val CY = 2015
      val result = await(gateway.updateExclusionsForEmployer("123/TEST1", 2015, 37).apply(mockrequest))
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must be(sampleBikJson)
    }

    " parse a response correctly and not mutate the returned response body - removal " in {
      val gateway = StubbedGateway
      val CY = 2015
      val result = await(gateway.removeExclusionForEmployer("123/TEST1", 2015, 37).apply(mockrequest))
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must be(sampleBikJson)
    }
  }

  "When removing exclusions the Controller " should {
    " parse a response correctly and not mutate the returned response body " in {
      val gateway = StubbedGateway
      val CY = 2015
      val result = await(gateway.removeExclusionForEmployer("123/TEST1", 2015, 37).apply(mockrequest))
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must be(sampleBikJson)
    }
  }

}
