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

package connectors

import config.PbikConfig
import controllers.utils.ControllerUtils
import helper.FakePBIKApplication
import models.v1.{BenefitListUpdateRequest, EmployerOptimisticLockRequest}
import models.{HeaderTags, PbikCredentials}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.libs.json.{JsString, JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, RequestId}

import java.util.regex.Pattern
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HmrcTierConnectorWrappedSpec extends AnyWordSpec with FakePBIKApplication with MockitoSugar with Matchers {

  val mockCredentials: PbikCredentials = PbikCredentials(1, 2, 3, "aoReference", "payeSchemeOperatorName")
  val uuidPattern: Pattern             = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".r.pattern

  trait Setup {

    val mockHttpClient: HttpClient       = mock[HttpClient]
    val pbikConfig: PbikConfig           = fakeApplication.injector.instanceOf[PbikConfig]
    val controllerUtils: ControllerUtils = fakeApplication.injector.instanceOf[ControllerUtils]
    val uuid                             = "8c5d7809-0eec-4257-b4ad-fe0125cefb2c"

    val connector: HmrcTierConnectorWrapped =
      new HmrcTierConnectorWrapped(mockHttpClient, pbikConfig, controllerUtils)

    val connectorWithMockUuid: HmrcTierConnectorWrapped =
      new HmrcTierConnectorWrapped(mockHttpClient, pbikConfig, controllerUtils) {
        override def generateNewUUID: String = uuid
      }
  }

  "MinimalAuthAction" when {

    ".generateNewUUID" should {
      "generate a non-null UUID" in new Setup {
        connector.generateNewUUID should not be None.orNull
      }

      "generate a UUID that matches the UUID format" in new Setup {
        uuidPattern.matcher(connector.generateNewUUID).matches() shouldBe true
      }
    }

    ".getCorrelationId" when {

      "requestID is present in the headerCarrier" should {
        "return new ID pre-appending the requestID when the requestID matches the format(8-4-4-4)" in new Setup {
          val requestId = "dcba0000-ij12-df34-jk56"
          connectorWithMockUuid.getCorrelationId(HeaderCarrier(requestId = Some(RequestId(requestId)))) shouldBe
            s"$requestId-${uuid.substring(24)}"
        }

        "return new ID when the requestID does not match the format(8-4-4-4)" in new Setup {
          val requestId = "1a2b-ij12-df34-jk56"
          connectorWithMockUuid.getCorrelationId(HeaderCarrier(requestId = Some(RequestId(requestId)))) shouldBe uuid
        }
      }

      "requestID is not present in the headerCarrier should return a new ID" should {
        "return the uuid" in new Setup {
          override val uuid: String = "123f4567-g89c-42c3-b456-557742330000"
          connectorWithMockUuid.getCorrelationId(HeaderCarrier()) shouldBe uuid
        }
      }
    }

    ".retrieveDataGet" when {
      "the HTTP GET request is successful" should {
        "return the HttpResponse" in new Setup {
          val url: String                               = "http://test.com"
          val headersResponse: Map[String, Seq[String]] = Map("key1" -> Seq("value1"))
          val expectedResponse: HttpResponse            = HttpResponse(Status.OK, json = Json.toJson("Success"), headersResponse)
          when(
            mockHttpClient.GET[HttpResponse](any[String], any[Seq[(String, String)]], any[Seq[(String, String)]])(
              any(),
              any(),
              any()
            )
          )
            .thenReturn(Future.successful(expectedResponse))

          val result: HttpResponse = await(connector.retrieveDataGet(url))

          result.status  shouldBe Status.OK
          result.body    shouldBe expectedResponse.json.toString()
          result.headers shouldBe headersResponse
        }
      }

      "the HTTP GET request fails" should {
        "return an HttpResponse with the exception message" in new Setup {
          val url: String              = "http://test.com"
          val exceptionMessage: String = "An error occurred"
          when(
            mockHttpClient.GET[HttpResponse](any[String], any[Seq[(String, String)]], any[Seq[(String, String)]])(
              any(),
              any(),
              any()
            )
          )
            .thenReturn(Future.failed(new Exception(exceptionMessage)))

          val result: HttpResponse = await(connector.retrieveDataGet(url))

          result.status  shouldBe Status.OK
          result.body    shouldBe JsString(exceptionMessage).toString()
          result.headers shouldBe Map.empty
        }
      }
    }

    ".retrieveDataPost" when {
      "the HTTP POST request is successful" should {
        "return the HttpResponse" in new Setup {
          implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

          val url: String                               = "http://test.com"
          val requestBody: JsValue                      = Json.toJson("Request Body")
          val headersResponse: Map[String, Seq[String]] = Map("key1" -> Seq("value1"))

          val expectedResponse: HttpResponse = HttpResponse(Status.OK, json = Json.toJson("Success"), headersResponse)
          when(
            mockHttpClient.POST[JsValue, HttpResponse](any[String], any[JsValue], any[Seq[(String, String)]])(
              any(),
              any(),
              any(),
              any()
            )
          )
            .thenReturn(Future.successful(expectedResponse))

          val result: HttpResponse = await(connector.retrieveDataPost(url, requestBody))

          result.status  shouldBe Status.OK
          result.body    shouldBe expectedResponse.json.toString()
          result.headers shouldBe headersResponse
        }
      }

      "the HTTP POST request fails" should {
        "return an HttpResponse with the exception message" in new Setup {
          implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

          val url: String              = "http://test.com"
          val requestBody: JsValue     = Json.toJson("Request Body")
          val exceptionMessage: String = "An error occurred"
          when(
            mockHttpClient.POST[JsValue, HttpResponse](any[String], any[JsValue], any[Seq[(String, String)]])(
              any(),
              any(),
              any(),
              any()
            )
          )
            .thenReturn(Future.failed(new Exception(exceptionMessage)))

          val result: HttpResponse = await(connector.retrieveDataPost(url, requestBody))

          result.status  shouldBe Status.OK
          result.json    shouldBe Json.toJson(exceptionMessage)
          result.headers shouldBe Map.empty
        }
      }
    }

    ".getRegisteredBenefits" when {

      "the HTTP GET request is successful" should {
        "return the HttpResponse" in new Setup {
          val headersResponse: Map[String, Seq[String]] = Map("key1" -> Seq("value1"))
          val year: Int                                 = 2022
          val expectedResponse: HttpResponse            = HttpResponse(Status.OK, json = Json.toJson("Success"), headersResponse)
          when(
            mockHttpClient.GET[HttpResponse](any[String], any[Seq[(String, String)]], any[Seq[(String, String)]])(
              any(),
              any(),
              any()
            )
          )
            .thenReturn(Future.successful(expectedResponse))

          val result: HttpResponse = await(connector.getRegisteredBenefits(mockCredentials, year))

          result.status  shouldBe Status.OK
          result.body    shouldBe expectedResponse.json.toString()
          result.headers shouldBe headersResponse
        }
      }

      "the HTTP GET request fails" should {
        "return an HttpResponse with the exception message" in new Setup {
          val year: Int                = 2022
          val exceptionMessage: String = "An error occurred"
          when(
            mockHttpClient.GET[HttpResponse](any[String], any[Seq[(String, String)]], any[Seq[(String, String)]])(
              any(),
              any(),
              any()
            )
          )
            .thenReturn(Future.failed(new Exception(exceptionMessage)))

          val result: HttpResponse = await(connector.getRegisteredBenefits(mockCredentials, year))

          result.status  shouldBe Status.OK
          result.body    shouldBe JsString(exceptionMessage).toString()
          result.headers shouldBe Map.empty
        }
      }
    }

    ".updateBenefitTypes" when {
      val lockRequest                                      = EmployerOptimisticLockRequest(
        HeaderTags.ETAG_DEFAULT_VALUE.toInt
      )
      val mockBikToUpdateRequest: BenefitListUpdateRequest = BenefitListUpdateRequest(List.empty, lockRequest)

      implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

      "the HTTP PUT request is successful" should {
        "return the HttpResponse" in new Setup {
          val url: String                               = "http://test.com"
          val headersResponse: Map[String, Seq[String]] = Map("key1" -> Seq("value1"))
          val expectedResponse: HttpResponse            = HttpResponse(Status.OK, json = Json.toJson("Success"), headersResponse)
          when(
            mockHttpClient.PUT[JsValue, HttpResponse](any[String], any[JsValue], any[Seq[(String, String)]])(
              any(),
              any(),
              any(),
              any()
            )
          )
            .thenReturn(Future.successful(expectedResponse))

          val result: HttpResponse = await(connector.updateBenefitTypes(url, mockBikToUpdateRequest))

          result.status  shouldBe Status.OK
          result.body    shouldBe expectedResponse.json.toString()
          result.headers shouldBe headersResponse
        }
      }

      "the HTTP PUT request fails" should {
        "return an HttpResponse with the exception message" in new Setup {
          val url: String              = "http://test.com"
          val exceptionMessage: String = "An error occurred"
          when(
            mockHttpClient.PUT[JsValue, HttpResponse](any[String], any[JsValue], any[Seq[(String, String)]])(
              any(),
              any(),
              any(),
              any()
            )
          )
            .thenReturn(Future.failed(new Exception(exceptionMessage)))

          val result: HttpResponse = await(connector.updateBenefitTypes(url, mockBikToUpdateRequest))

          result.status  shouldBe Status.OK
          result.body    shouldBe JsString(exceptionMessage).toString()
          result.headers shouldBe Map.empty
        }
      }
    }

  }

}
