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
import helper.{FakePBIKApplication, TestMinimalAuthAction}
import models.{HeaderTags, PbikCredentials}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Request, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ControllersSpec extends PlaySpec with FakePBIKApplication with Results {

  implicit lazy override val app: Application = new GuiceApplicationBuilder()
    .configure(config)
    .overrides(bind(classOf[HmrcTierConnectorWrapped]).toInstance(mock(classOf[HmrcTierConnectorWrapped])))
    .overrides(bind(classOf[ControllerUtils]).toInstance(mock(classOf[ControllerUtils])))
    .overrides(bind(classOf[MinimalAuthAction]).to(classOf[TestMinimalAuthAction]))
    .build()

  val mockCredentials: PbikCredentials = mock(classOf[PbikCredentials])

  val paye_scheme_type: Int             = 123
  val employer_number: String           = "999"
  val paye_seq_no: Int                  = 123
  val empref                            = "123/TEST1"
  val mockMutators: Map[String, String] = Map[String, String]()
  val ibdtype                           = 39
  val cy: Int                           = TaxYear.current.currentYear
  val year: Int                         = cy + 1

  val npsHeaders: Map[String, String] = HeaderTags.createResponseHeaders("1", "0")

  val mockGatewayNPSController: GatewayNPSController = {
    val gnc: GatewayNPSController = app.injector.instanceOf[GatewayNPSController]

    when(
      gnc.controllerUtils
        .retrieveNPSCredentials(any(), anyInt, anyString)(any[HeaderCarrier])
    )
      .thenReturn(Future(mockCredentials))

    when(gnc.controllerUtils.getNPSMutatorSessionHeader(any[Request[AnyContent]]))
      .thenReturn(mockMutators)

    when(gnc.controllerUtils.generateResultBasedOnStatus(any())(any()))
      .thenReturn(Future.successful(Ok("").withHeaders(npsHeaders.toSeq: _*)))

    when(gnc.controllerUtils.mapResponseToResult(any()))
      .thenReturn(Future.successful(Ok("").withHeaders(npsHeaders.toSeq: _*)))

    gnc
  }

  implicit lazy val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  "A post request" should {

    "Successfully update registered benefits " in {
      val result = mockGatewayNPSController.updateBenefitTypes(empref, year)(request)
      status(result)          must be(OK)
      contentAsString(result) must be("")
      headers(result)         must be(npsHeaders)
    }

    "Successfully update registered benefits, If invalid json defaults to empty List" in {
      val requestWithBody = FakeRequest().withJsonBody(Json.parse("""{"invalid": "json"}"""))
      val result          = mockGatewayNPSController.updateBenefitTypes(empref, year)(requestWithBody)
      status(result)          must be(OK)
      contentAsString(result) must be("")
      headers(result)         must be(npsHeaders)
    }

    "Successfully update registered exclusions" in {
      val result = mockGatewayNPSController.updateExclusionsForEmployer(empref, year, ibdtype)(request)
      status(result)          must be(OK)
      contentAsString(result) must be("")
      headers(result)         must be(npsHeaders)
    }
  }
}
