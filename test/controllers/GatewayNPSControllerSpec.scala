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

import config.PbikConfig
import connectors.NpsConnector
import helper.{FakePBIKApplication, TestMinimalAuthAction}
import models.v1.PbikCredentials
import org.mockito.ArgumentMatchers.{any, anyInt, anyString}
import org.mockito.Mockito.{mock, when}
import org.scalatest.Inspectors.forAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsJson, BodyParsers, Result}
import play.api.test.Helpers.{await, defaultAwaitTimeout, stubControllerComponents}
import play.api.test.{FakeRequest, Helpers}
import play.mvc.Http.Status
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GatewayNPSControllerSpec extends AnyWordSpec with FakePBIKApplication with Matchers {

  private val fakeRequest: FakeRequest[AnyContentAsEmpty.type]   = FakeRequest("GET", "/test")
  private val fakeRequestWithBody: FakeRequest[AnyContentAsJson] =
    FakeRequest("POST", "/test").withJsonBody(Json.obj("key" -> "value"))
  private val parser: BodyParsers.Default                        = app.injector.instanceOf[BodyParsers.Default]

  private val uuid = "8c5d7809-0eec-4257-b4ad-fe0125cefb2d"

  private val headersResponse: Map[String, Seq[String]] = Map(
    "key1" -> Seq("value1"),
    "key3" -> Seq("value1", "value2"),
    "key2" -> Seq("value1")
  )

  def expectedResponse(status: Int, body: JsValue): HttpResponse = HttpResponse(status, body, headersResponse)
  def expectedResponse(status: Int): HttpResponse                = expectedResponse(status, Json.toJson("Success"))

  def assertResult(result: Result, status: Int): Unit = {
    val expectedHeaders = headersResponse
      .flatMap { case (key, values) => values.map(value => (key, value)) }
      .toSeq
      .sortBy(_._1)
    val resultF         = Future.successful(result)

    Helpers.status(resultF)          shouldBe status
    Helpers.contentAsString(resultF) shouldBe expectedResponse(status).json.toString()
    Helpers.headers(resultF).toSeq   shouldBe expectedHeaders
  }

  trait Setup {
    private val authAction             = new TestMinimalAuthAction(parser)
    val pbikConfig: PbikConfig         = app.injector.instanceOf[PbikConfig]
    val mockNpsConnector: NpsConnector = mock(classOf[NpsConnector])

    when(mockNpsConnector.generateNewUUID).thenReturn(uuid)

    val controller = new GatewayNPSController(mockNpsConnector, authAction, stubControllerComponents())

    def mockGetPbikCredentials(response: Future[PbikCredentials]): Unit =
      when(mockNpsConnector.getPbikCredentials(anyString(), anyString())(any())).thenReturn(response)
  }

  "GatewayNPSController" when {

    ".getBenefitTypes" when {
      forAll(allPlayFrameworkStatusCodes) { status =>
        s"return the $status Result" in new Setup {
          when(mockNpsConnector.getBenefitTypes(anyInt())(any()))
            .thenReturn(Future.successful(expectedResponse(status)))
          val result: Result = await(controller.getBenefitTypes(2020)(fakeRequest))
          assertResult(result, status)
        }
      }
    }

    ".getRegisteredBenefits" when {
      forAll(allPlayFrameworkStatusCodes) { status =>
        s"return the $status Result" in new Setup {
          mockGetPbikCredentials(Future.successful(mockCredentials))
          when(mockNpsConnector.getRegisteredBenefits(any(), anyInt())(any()))
            .thenReturn(Future.successful(expectedResponse(status)))
          val result: Result =
            await(controller.getRegisteredBenefits("fake_office_number", "fake_office_reference", 2020)(fakeRequest))
          assertResult(result, status)
        }
      }

      "return exception when getPbikCredentials fails" in new Setup {
        mockGetPbikCredentials(Future.failed(new Exception("Failed to get credentials")))
        val exception: Exception = intercept[Exception](
          await(controller.getRegisteredBenefits("fake_office_number", "fake_office_reference", 2020)(fakeRequest))
        )
        exception.getMessage shouldBe "Failed to get credentials"
      }
    }

    ".getExclusionsForEmployer" when {
      forAll(allPlayFrameworkStatusCodes) { status =>
        s"return the $status Result" in new Setup {
          mockGetPbikCredentials(Future.successful(mockCredentials))
          when(mockNpsConnector.getAllExcludedPeopleForABenefit(any(), anyInt(), anyString())(any()))
            .thenReturn(Future.successful(expectedResponse(status)))
          val result: Result =
            await(
              controller.getExclusionsForEmployer("fake_office_number", "fake_office_reference", 2020, "iabd")(
                fakeRequest
              )
            )
          assertResult(result, status)
        }
      }

      "return exception when getPbikCredentials fails" in new Setup {
        mockGetPbikCredentials(Future.failed(new Exception("Failed to get credentials")))
        val exception: Exception = intercept[Exception](
          await(
            controller.getExclusionsForEmployer("fake_office_number", "fake_office_reference", 2020, "iabd")(
              fakeRequest
            )
          )
        )
        exception.getMessage shouldBe "Failed to get credentials"
      }
    }

    ".updateBenefitTypes" when {
      forAll(allPlayFrameworkStatusCodes) { status =>
        s"return the $status Result" in new Setup {
          mockGetPbikCredentials(Future.successful(mockCredentials))
          when(mockNpsConnector.updateBenefitTypes(any(), anyInt(), any())(any()))
            .thenReturn(Future.successful(expectedResponse(status)))
          val result: Result =
            await(
              controller.updateBenefitTypes("fake_office_number", "fake_office_reference", 2020)(fakeRequestWithBody)
            )
          assertResult(result, status)
        }
      }

      s"return the OK Result when no body" in new Setup {
        mockGetPbikCredentials(Future.successful(mockCredentials))
        when(mockNpsConnector.updateBenefitTypes(any(), anyInt(), any())(any()))
          .thenReturn(Future.successful(expectedResponse(Status.OK)))
        val result: Result =
          await(
            controller.updateBenefitTypes("fake_office_number", "fake_office_reference", 2020)(
              fakeRequest
            )
          )
        assertResult(result, Status.OK)
      }

      "return exception when getPbikCredentials fails" in new Setup {
        mockGetPbikCredentials(Future.failed(new Exception("Failed to get credentials")))
        val exception: Exception = intercept[Exception](
          await(controller.updateBenefitTypes("fake_office_number", "fake_office_reference", 2020)(fakeRequestWithBody))
        )
        exception.getMessage shouldBe "Failed to get credentials"
      }
    }

    ".updateExclusionsForEmployer" when {
      forAll(allPlayFrameworkStatusCodes) { status =>
        s"return the $status Result" in new Setup {
          mockGetPbikCredentials(Future.successful(mockCredentials))
          when(mockNpsConnector.updateExcludedPeopleForABenefit(any(), anyInt(), any())(any()))
            .thenReturn(Future.successful(expectedResponse(status)))
          val result: Result =
            await(
              controller.updateExclusionsForEmployer("fake_office_number", "fake_office_reference", 2020)(
                fakeRequestWithBody
              )
            )
          assertResult(result, status)
        }
      }

      s"return the OK Result when no body" in new Setup {
        mockGetPbikCredentials(Future.successful(mockCredentials))
        when(mockNpsConnector.updateExcludedPeopleForABenefit(any(), anyInt(), any())(any()))
          .thenReturn(Future.successful(expectedResponse(Status.OK)))
        val result: Result =
          await(
            controller.updateExclusionsForEmployer("fake_office_number", "fake_office_reference", 2020)(
              fakeRequest
            )
          )
        assertResult(result, Status.OK)
      }

      "return exception when getPbikCredentials fails" in new Setup {
        mockGetPbikCredentials(Future.failed(new Exception("Failed to get credentials")))
        val exception: Exception = intercept[Exception](
          await(
            controller.updateExclusionsForEmployer("fake_office_number", "fake_office_reference", 2020)(
              fakeRequestWithBody
            )
          )
        )
        exception.getMessage shouldBe "Failed to get credentials"
      }
    }

    ".removeExclusionForEmployer" when {
      forAll(allPlayFrameworkStatusCodes) { status =>
        s"return the $status Result" in new Setup {
          mockGetPbikCredentials(Future.successful(mockCredentials))
          when(mockNpsConnector.removeExcludedPeopleForABenefit(any(), anyInt(), anyString(), any())(any()))
            .thenReturn(Future.successful(expectedResponse(status)))
          val result: Result =
            await(
              controller.removeExclusionForEmployer("fake_office_number", "fake_office_reference", 2020, "fake_iabd")(
                fakeRequestWithBody
              )
            )
          assertResult(result, status)
        }
      }

      s"return the OK Result when no body" in new Setup {
        mockGetPbikCredentials(Future.successful(mockCredentials))
        when(mockNpsConnector.removeExcludedPeopleForABenefit(any(), anyInt(), anyString(), any())(any()))
          .thenReturn(Future.successful(expectedResponse(Status.OK)))
        val result: Result =
          await(
            controller.removeExclusionForEmployer("fake_office_number", "fake_office_reference", 2020, "fake_iabd")(
              fakeRequest
            )
          )
        assertResult(result, Status.OK)
      }

      "return exception when getPbikCredentials fails" in new Setup {
        mockGetPbikCredentials(Future.failed(new Exception("Failed to get credentials")))
        val exception: Exception = intercept[Exception](
          await(
            controller.removeExclusionForEmployer("fake_office_number", "fake_office_reference", 2020, "fake_iabd")(
              fakeRequestWithBody
            )
          )
        )
        exception.getMessage shouldBe "Failed to get credentials"
      }
    }

    ".tracePeopleByPersonalDetails" when {
      forAll(allPlayFrameworkStatusCodes) { status =>
        s"return the $status Result" in new Setup {
          mockGetPbikCredentials(Future.successful(mockCredentials))
          when(mockNpsConnector.tracePeopleByPersonalDetails(any(), anyInt(), any())(any()))
            .thenReturn(Future.successful(expectedResponse(status)))
          val result: Result =
            await(
              controller.tracePeopleByPersonalDetails("fake_office_number", "fake_office_reference", 2020)(
                fakeRequestWithBody
              )
            )
          assertResult(result, status)
        }
      }

      s"return the OK Result when no body" in new Setup {
        mockGetPbikCredentials(Future.successful(mockCredentials))
        when(mockNpsConnector.tracePeopleByPersonalDetails(any(), anyInt(), any())(any()))
          .thenReturn(Future.successful(expectedResponse(Status.OK)))
        val result: Result =
          await(
            controller.tracePeopleByPersonalDetails("fake_office_number", "fake_office_reference", 2020)(
              fakeRequest
            )
          )
        assertResult(result, Status.OK)
      }

      "return exception when getPbikCredentials fails" in new Setup {
        mockGetPbikCredentials(Future.failed(new Exception("Failed to get credentials")))
        val exception: Exception = intercept[Exception](
          await(
            controller.tracePeopleByPersonalDetails("fake_office_number", "fake_office_reference", 2020)(
              fakeRequestWithBody
            )
          )
        )
        exception.getMessage shouldBe "Failed to get credentials"
      }
    }

  }

}
