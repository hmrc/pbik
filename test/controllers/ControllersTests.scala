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

import _root_.play.api.test.FakeRequest
import connectors.HmrcTierConnectorWrapped
import controllers.utils.ControllerUtilsWrapped
import models.{HeaderTags, PbikCredentials}
import org.scalatest.mock.MockitoSugar
import org.specs2.mock.mockito.MockitoMatchers
import play.api.libs.json.{ Json}
import play.api.libs.ws.WSResponse
import play.api.mvc.{AnyContent, Request}
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.HttpResponse
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.time.TaxYearResolver
import org.mockito.Mockito._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ControllersTests extends UnitSpec with MockitoSugar with MockitoMatchers with FakePBIKApplication {

  val mockCredentials = mock[PbikCredentials]
  val paye_scheme_type: Int = 123
  val employer_number: String = "999"
  val paye_seq_no: Int = 123
  val empref = "123/TEST1"
  val mockMutators:Map[String, String] = Map[String,String]()
  val ibdtype = 39
  val year = TaxYearResolver.currentTaxYear+1

  class MockNPSController extends GatewayNPSController with HttpResponse {

    val mockControllerUtils = mock[ControllerUtilsWrapped]
    when(mockControllerUtils.retrieveNPSCredentials(any, anyInt, anyString)(any[Request[AnyContent]], any[HeaderCarrier], any)).thenReturn(mockCredentials)
    when(mockControllerUtils.getNPSMutatorSessionHeader(any[Request[AnyContent]], any[HeaderCarrier])).thenReturn(Future(Some(mockMutators)))

    override lazy val baseURL: String = ""
    override val controllerUtils = mockControllerUtils
    override val tierConnector = mock[HmrcTierConnectorWrapped]

    val mockHeaders = Map[String, String]()

    when(mockControllerUtils.generateResultBasedOnStatus(any)(any, any, any)).thenReturn(
      Future.successful(Ok("").withHeaders(HeaderTags.ETAG -> "1", HeaderTags.X_TXID -> "0")))
  }

  def mockGatewayNPSController = new MockNPSController

  def mockCYSupportedGatewayNPSController = new MockNPSController {
    override val cyEnabled = true
  }

  implicit val request = FakeRequest()

  "A post request" should {

    "Successfully update registered benefits " in {
      running(fakeApplication) {
        val result = await(mockGatewayNPSController.updateBenefitTypes(empref, year)(request))
        bodyOf(result) shouldBe ""
        status(result) shouldBe 200
        headers(result) shouldBe Map("Content-Type" -> "text/plain; charset=utf-8", HeaderTags.ETAG -> "1", HeaderTags.X_TXID -> "0")
      }
    }

    "Successfully update registered exclusions" in {
      running(fakeApplication) {
        val result = await(mockGatewayNPSController.updateExclusionsForEmployer(empref, year, ibdtype)(request))
        bodyOf(result) shouldBe ""
        status(result) shouldBe 200
        headers(result) shouldBe Map("Content-Type" -> "text/plain; charset=utf-8", HeaderTags.ETAG -> "1", HeaderTags.X_TXID -> "0")
      }
    }

  }

  "When CY mode is disabled and a call is made to update a benefit for that year, the controller " should {
      "not allow the call to proceed " in {
        running(fakeApplication) {
          val valid = mockGatewayNPSController.cyCheck(TaxYearResolver.currentTaxYear)
          valid shouldBe false
        }

    }

  }

  "When CY mode is disabled and a call is made to update next year, the controller " should {
    "allow the call to proceed " in {
      running(fakeApplication) {
        val valid = mockGatewayNPSController.cyCheck(TaxYearResolver.currentTaxYear+1)
        valid shouldBe true
      }

    }

  }

  "When CY mode is enabled and a call is made to update a benefit for that year, the controller " should {

    "allow the call to proceed " in {
      running(fakeApplication) {
        val valid = mockCYSupportedGatewayNPSController.cyCheck(TaxYearResolver.currentTaxYear)
        valid shouldBe true
      }
    }

  }

  "When CY mode is enabled and a call is made to update the next year, the controller " should {

    "allow the call to proceed " in {
      running(fakeApplication) {
        val valid = mockCYSupportedGatewayNPSController.cyCheck(TaxYearResolver.currentTaxYear+1)
        valid shouldBe true
      }

    }
  }
}
