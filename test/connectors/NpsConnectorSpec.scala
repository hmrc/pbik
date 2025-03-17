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
import helper.FakePBIKApplication
import models.v1.PbikCredentials
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, verify, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.Inspectors.forAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, RequestId}

import java.util.regex.Pattern
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.Try

class NpsConnectorSpec extends AnyWordSpec with FakePBIKApplication with Matchers {

  private val uuidPattern: Pattern =
    "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".r.pattern

  private val mockJsonBody: JsValue = Json.parse("""{"key": "value"}""")

  private def getAllHeadersFor(mockRequestBuilder: RequestBuilder): Iterable[(String, String)] = {
    val headerCapture: ArgumentCaptor[(String, String)] = ArgumentCaptor.forClass(classOf[(String, String)])
    // ignore failure in case we are not able to capture the headers return empty list
    Try(verify(mockRequestBuilder).setHeader(headerCapture.capture()))
    headerCapture.getAllValues.asScala
  }

  private val headersResponse: Map[String, Seq[String]]                  = Map("key1" -> Seq("value1"))
  private def expectedResponse(status: Int, body: JsValue): HttpResponse = HttpResponse(status, body, headersResponse)
  private def expectedResponse(status: Int): HttpResponse                = expectedResponse(status, Json.toJson("Success"))

  trait Setup {

    val mockHttpClient: HttpClientV2           = mock(classOf[HttpClientV2])
    val mockRequestBuilderGet: RequestBuilder  = mock(classOf[RequestBuilder])
    val mockRequestBuilderPost: RequestBuilder = mock(classOf[RequestBuilder])
    val mockRequestBuilderPut: RequestBuilder  = mock(classOf[RequestBuilder])
    val pbikConfig: PbikConfig                 = app.injector.instanceOf[PbikConfig]
    val uuid: String                           = "8c5d7809-0eec-4257-b4ad-fe0125cefb2c"

    val connector: NpsConnector =
      new NpsConnector(mockHttpClient, pbikConfig)

    val connectorWithMockUuid: NpsConnector =
      new NpsConnector(mockHttpClient, pbikConfig) {
        override def generateNewUUID: String = uuid
      }

    when(mockHttpClient.get(any())(any())).thenReturn(mockRequestBuilderGet)
    when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilderPost)
    when(mockHttpClient.put(any())(any())).thenReturn(mockRequestBuilderPut)

    when(mockRequestBuilderGet.setHeader(any())).thenReturn(mockRequestBuilderGet)
    when(mockRequestBuilderPost.setHeader(any())).thenReturn(mockRequestBuilderPost)
    when(mockRequestBuilderPut.setHeader(any())).thenReturn(mockRequestBuilderPut)

    private def mockExecute(
      builder: RequestBuilder,
      expectedResponse: Future[HttpResponse]
    ): OngoingStubbing[Future[HttpResponse]] =
      when(builder.execute(any[HttpReads[HttpResponse]], any())).thenReturn(expectedResponse)

    def mockGetEndpoint(expectedResponse: Future[HttpResponse]): OngoingStubbing[Future[HttpResponse]] =
      mockExecute(mockRequestBuilderGet, expectedResponse)

    def mockPostEndpoint(expectedResponse: Future[HttpResponse]): OngoingStubbing[RequestBuilder] = {
      mockExecute(mockRequestBuilderPost, expectedResponse)
      when(mockRequestBuilderPost.withBody(any[JsValue])(any(), any(), any())).thenReturn(mockRequestBuilderPost)
    }

    def mockPutEndpoint(expectedResponse: Future[HttpResponse]): OngoingStubbing[RequestBuilder] = {
      mockExecute(mockRequestBuilderPut, expectedResponse)
      when(mockRequestBuilderPut.withBody(any[JsValue])(any(), any(), any())).thenReturn(mockRequestBuilderPut)
    }

    def gatherHeaderMockInfo(): Iterable[(String, String)] = {
      val getHeaders  = getAllHeadersFor(mockRequestBuilderGet)
      val postHeaders = getAllHeadersFor(mockRequestBuilderPost)
      val putHeaders  = getAllHeadersFor(mockRequestBuilderPut)
      (getHeaders ++ postHeaders ++ putHeaders).toSet
    }

    val buildExpectedHeaders: Seq[(String, String)] = Seq(
      pbikConfig.serviceOriginatorIdKeyV1 -> pbikConfig.serviceOriginatorIdV1,
      "CorrelationId"                     -> uuid,
      "Authorization"                     -> s"Basic ${pbikConfig.authorizationToken}"
    )

    def assertResult(result: HttpResponse, status: Int, expectedHeaders: Seq[(String, String)]): Unit = {
      result.status               shouldBe status
      result.body                 shouldBe expectedResponse(status).json.toString()
      gatherHeaderMockInfo().head shouldBe expectedHeaders
    }

  }

  "NPSConnectorSpec" when {

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
          val requestId = "8c5d7809-0eec-4257-b4ad"
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

    ".getRegisteredBenefits" when
      forAll(allPlayFrameworkStatusCodes) { status =>
        s"return the $status HttpResponse" in new Setup {
          mockGetEndpoint(Future.successful(expectedResponse(status)))
          val result: HttpResponse =
            await(connectorWithMockUuid.getRegisteredBenefits(mockCredentials, 2020))
          assertResult(result, status, buildExpectedHeaders)
        }
      }

    ".updateBenefitTypes" when
      forAll(allPlayFrameworkStatusCodes) { status =>
        s"return the $status HttpResponse" in new Setup {
          mockPutEndpoint(Future.successful(expectedResponse(status)))
          val result: HttpResponse =
            await(connectorWithMockUuid.updateBenefitTypes(mockCredentials, 2020, mockJsonBody))
          assertResult(result, status, buildExpectedHeaders)
        }
      }

    ".getBenefitTypes" when
      forAll(allPlayFrameworkStatusCodes) { status =>
        s"return the $status HttpResponse" in new Setup {
          mockGetEndpoint(Future.successful(expectedResponse(status)))
          val result: HttpResponse = await(connectorWithMockUuid.getBenefitTypes(2020))
          assertResult(result, status, buildExpectedHeaders)
        }
      }

    ".getPbikCredentials" when {
      forAll(allPlayFrameworkStatusCodes) { status =>
        s"return the $status HttpResponse" in new Setup {
          mockGetEndpoint(Future.successful(expectedResponse(status, Json.toJson(mockCredentials))))
          val result: PbikCredentials =
            await(connectorWithMockUuid.getPbikCredentials("tax office test", "tax reference test", 2020))
          result shouldBe mockCredentials
        }
      }

      "the HTTP GET request fails" should {
        "return an HttpResponse with the exception message" in new Setup {
          val exceptionMessage: String = "An error occurred"
          mockGetEndpoint(Future.failed(new Exception(exceptionMessage)))

          intercept[Exception](
            await(connectorWithMockUuid.getPbikCredentials("tax office test", "tax reference test", 2020))
          )
        }
      }

      "the HTTP GET request successful" should {
        "return an HttpResponse with wrong body" in new Setup {
          mockGetEndpoint(Future.successful(expectedResponse(Status.OK, Json.toJson("Wrong body"))))

          val exception: IllegalArgumentException = intercept[IllegalArgumentException](
            await(connectorWithMockUuid.getPbikCredentials("tax office test", "tax reference test", 2020))
          )

          exception.getMessage should startWith("Invalid JSON received from NPS")
        }
      }
    }

    ".getAllExcludedPeopleForABenefit" when
      forAll(allPlayFrameworkStatusCodes) { status =>
        s"return the $status HttpResponse" in new Setup {
          mockGetEndpoint(Future.successful(expectedResponse(status)))
          val result: HttpResponse = await(
            connectorWithMockUuid.getAllExcludedPeopleForABenefit(mockCredentials, 2020, "iabd")
          )
          assertResult(result, status, buildExpectedHeaders)
        }
      }

    ".updateExcludedPeopleForABenefit" when
      forAll(allPlayFrameworkStatusCodes) { status =>
        s"return the $status HttpResponse" in new Setup {
          mockPostEndpoint(Future.successful(expectedResponse(status)))
          val result: HttpResponse = await(
            connectorWithMockUuid
              .updateExcludedPeopleForABenefit(mockCredentials, 2020, mockJsonBody)
          )
          assertResult(result, status, buildExpectedHeaders)
        }
      }

    ".removeExcludedPeopleForABenefit" when
      forAll(allPlayFrameworkStatusCodes) { status =>
        s"return the $status HttpResponse" in new Setup {
          mockPostEndpoint(Future.successful(expectedResponse(status)))
          val result: HttpResponse = await(
            connectorWithMockUuid
              .removeExcludedPeopleForABenefit(mockCredentials, 2020, fakeIabd, mockJsonBody)
          )
          assertResult(result, status, buildExpectedHeaders)
        }
      }

    ".tracePeople" when
      forAll(allPlayFrameworkStatusCodes) { status =>
        s"return the $status HttpResponse" in new Setup {
          mockPostEndpoint(Future.successful(expectedResponse(status)))
          val result: HttpResponse = await(
            connectorWithMockUuid
              .tracePeople(mockCredentials, 2020, mockJsonBody)
          )
          assertResult(result, status, buildExpectedHeaders)
        }
      }

  }

}
