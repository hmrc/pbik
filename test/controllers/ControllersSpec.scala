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

import _root_.play.api.test.FakeRequest
import connectors.HmrcTierConnectorWrapped
import controllers.actions.MinimalAuthAction
import controllers.utils.ControllerUtils
import helper.{CYEnabledSetup, TestMinimalAuthAction}
import models.{HeaderTags, PbikCredentials}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.http.HttpEntity.Strict
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Request, Results}
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ControllersSpec extends PlaySpec with MockitoSugar with FakePBIKApplication with Results {

  implicit lazy override val app: Application = new GuiceApplicationBuilder()
    .configure(config)
    .overrides(bind(classOf[HmrcTierConnectorWrapped]).toInstance(mock[HmrcTierConnectorWrapped]))
    .overrides(bind(classOf[ControllerUtils]).toInstance(mock[ControllerUtils]))
    .overrides(bind(classOf[MinimalAuthAction]).to(classOf[TestMinimalAuthAction]))
    .build()

  val mockCredentials: PbikCredentials = mock[PbikCredentials]

  val paye_scheme_type: Int = 123
  val employer_number: String = "999"
  val paye_seq_no: Int = 123
  val empref = "123/TEST1"
  val mockMutators: Map[String, String] = Map[String, String]()
  val ibdtype = 39
  val year: Int = TaxYear.current.currentYear + 1

  val mockGatewayNPSController: GatewayNPSController = {
    val gnc: GatewayNPSController = app.injector.instanceOf[GatewayNPSController]

    when(
      gnc.controllerUtils
        .retrieveNPSCredentials(any(), anyInt, anyString)(any[Request[AnyContent]], any[HeaderCarrier], any()))
      .thenReturn(Future(mockCredentials))

    when(gnc.controllerUtils.getNPSMutatorSessionHeader(any[Request[AnyContent]], any[HeaderCarrier]))
      .thenReturn(Future(Some(mockMutators)))

    when(gnc.controllerUtils.generateResultBasedOnStatus(any())(any(), any(), any()))
      .thenReturn(Future.successful(Ok("").withHeaders(HeaderTags.ETAG -> "1", HeaderTags.X_TXID -> "0")))

    gnc
  }

  implicit lazy val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  "A post request" should {

    "Successfully update registered benefits " in {
      val result = await(mockGatewayNPSController.updateBenefitTypes(empref, year)(request))
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must be("")
      result.header.headers must be(Map(HeaderTags.ETAG -> "1", HeaderTags.X_TXID -> "0"))
      //bodyOf(result) shouldBe ""
      //status(result) shouldBe 200
      //headers(result) shouldBe Map("Content-Type" -> "text/plain; charset=utf-8", HeaderTags.ETAG -> "1", HeaderTags.X_TXID -> "0")
    }

    "Successfully update registered exclusions" in {
      val result = await(mockGatewayNPSController.updateExclusionsForEmployer(empref, year, ibdtype)(request))
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must be("")
      result.header.headers must be(Map(HeaderTags.ETAG -> "1", HeaderTags.X_TXID -> "0"))
      //bodyOf(result) shouldBe ""
      //status(result) shouldBe 200
      //headers(result) shouldBe Map("Content-Type" -> "text/plain; charset=utf-8", HeaderTags.ETAG -> "1", HeaderTags.X_TXID -> "0")
    }
  }

  "When CY mode is disabled and a call is made to update a benefit for that year, the controller " should {
    "not allow the call to proceed " in {
      val valid = mockGatewayNPSController.cyCheck(TaxYear.current.currentYear)
      valid must be(false)
    }
  }

  "When CY mode is disabled and a call is made to update next year, the controller " should {
    "allow the call to proceed " in {
      val valid = mockGatewayNPSController.cyCheck(TaxYear.current.currentYear + 1)
      valid must be(true)
    }
  }

  "When CY mode is enabled and a call is made to update a benefit for that year, the controller " should {
    "allow the call to proceed " in new CYEnabledSetup {
      val injector: Injector = new GuiceApplicationBuilder()
        .overrides(GuiceTestModule)
        .injector()

      val mockCYSupportedGatewayNPSController: GatewayNPSController = {
        val mcysgnc = injector.instanceOf[GatewayNPSController]

        when(
          mcysgnc.controllerUtils
            .retrieveNPSCredentials(any(), anyInt, anyString)(any[Request[AnyContent]], any[HeaderCarrier], any()))
          .thenReturn(Future(mockCredentials))

        when(mcysgnc.controllerUtils.getNPSMutatorSessionHeader(any[Request[AnyContent]], any[HeaderCarrier]))
          .thenReturn(Future(Some(mockMutators)))

        when(mcysgnc.controllerUtils.generateResultBasedOnStatus(any())(any(), any(), any()))
          .thenReturn(Future.successful(Ok("").withHeaders(HeaderTags.ETAG -> "1", HeaderTags.X_TXID -> "0")))

        when(mcysgnc.configuration.cyEnabled).thenReturn(true)

        mcysgnc
      }

      val valid: Boolean = mockCYSupportedGatewayNPSController.cyCheck(TaxYear.current.currentYear)
      valid must be(true)
    }
  }

  "When CY mode is enabled and a call is made to update the next year, the controller " should {
    "allow the call to proceed " in new CYEnabledSetup {
      val injector: Injector = new GuiceApplicationBuilder()
        .overrides(GuiceTestModule)
        .injector()

      val mockCYSupportedGatewayNPSController: GatewayNPSController = {
        val mcysgnc = injector.instanceOf[GatewayNPSController]

        when(
          mcysgnc.controllerUtils
            .retrieveNPSCredentials(any(), anyInt, anyString)(any[Request[AnyContent]], any[HeaderCarrier], any()))
          .thenReturn(Future(mockCredentials))

        when(mcysgnc.controllerUtils.getNPSMutatorSessionHeader(any[Request[AnyContent]], any[HeaderCarrier]))
          .thenReturn(Future(Some(mockMutators)))

        when(mcysgnc.controllerUtils.generateResultBasedOnStatus(any())(any(), any(), any()))
          .thenReturn(Future.successful(Ok("").withHeaders(HeaderTags.ETAG -> "1", HeaderTags.X_TXID -> "0")))

        when(mcysgnc.configuration.cyEnabled).thenReturn(true)

        mcysgnc
      }

      val valid: Boolean = mockCYSupportedGatewayNPSController.cyCheck(TaxYear.current.currentYear + 1)
      valid must be(true)
    }
  }
}
