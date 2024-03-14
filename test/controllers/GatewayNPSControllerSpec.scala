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

import connectors.HmrcTierConnectorWrapped
import controllers.actions.MinimalAuthAction
import controllers.utils.ControllerUtils
import helper.{FakePBIKApplication, StubbedControllerUtils, TestMinimalAuthAction}
import models.v1.BenefitListUpdateRequest
import models.{Bik, PbikCredentials}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import play.api.test.Helpers._
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

    override def status: Int = Status.OK

    override val json: JsValue = Json.parse(sampleBikJson)
    override val body: String  = sampleBikJson
  }

  val StubbedGateway: GatewayNPSController = {

    val gnc = app.injector.instanceOf[GatewayNPSController]

    when(gnc.tierConnector.retrieveDataGet(anyString)(any[HeaderCarrier]))
      .thenReturn(Future.successful(new FakeResponse))
    when(
      gnc.tierConnector.retrieveDataPost(anyString, any[JsValue])(
        any[HeaderCarrier],
        any[Request[_]]
      )
    )
      .thenReturn(Future.successful(new FakeResponse))

    when(gnc.tierConnector.getRegisteredBenefits(any[PbikCredentials], anyInt())(any[HeaderCarrier]))
      .thenReturn(Future.successful(new FakeResponse))
    when(
      gnc.tierConnector
        .updateBenefitTypes(anyString, any[BenefitListUpdateRequest])(any[HeaderCarrier], any[Request[_]])
    )
      .thenReturn(Future.successful(new FakeResponse))
    gnc
  }

  "When getting Benefits Types the Controller " should {
    " parse a response correctly and not mutate the returned response body " in {
      val gateway = StubbedGateway
      val result  = gateway.getRegisteredBenefits("123/TEST1", 2015).apply(mockrequest)

      status(result)                     must be(OK)
      contentAsJson(result).as[Seq[Bik]] must be(biks)
    }
  }

  "When getting exclusions the Controller " should {
    " parse a response correctly and not mutate the returned response body " in {
      val gateway = StubbedGateway
      val result  = gateway.getExclusionsForEmployer("123/TEST1", 2015, 37).apply(mockrequest)
      status(result)          must be(OK)
      contentAsString(result) must be(sampleBikJson)
    }
  }

  "When updating exclusions the Controller " should {
    " parse a response correctly and not mutate the returned response body - update " in {
      val gateway = StubbedGateway
      val result  = gateway.updateExclusionsForEmployer("123/TEST1", 2015, 37).apply(mockrequest)
      status(result)          must be(OK)
      contentAsString(result) must be(sampleBikJson)
    }

    " parse a response correctly and not mutate the returned response body - removal " in {
      val gateway = StubbedGateway
      val result  = gateway.removeExclusionForEmployer("123/TEST1", 2015, 37).apply(mockrequest)
      status(result)          must be(OK)
      contentAsString(result) must be(sampleBikJson)
    }
  }

  "When removing exclusions the Controller " should {
    " parse a response correctly and not mutate the returned response body " in {
      val gateway = StubbedGateway
      val result  = gateway.removeExclusionForEmployer("123/TEST1", 2015, 37).apply(mockrequest)
      status(result)          must be(OK)
      contentAsString(result) must be(sampleBikJson)
    }
  }

}
