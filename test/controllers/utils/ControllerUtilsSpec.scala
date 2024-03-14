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

package controllers.utils

import connectors.HmrcTierConnectorWrapped
import helper.FakePBIKApplication
import models.v1.{NPSError, NPSErrors}
import models.{HeaderTags, PbikCredentials, PbikError}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsValue, Json, OFormat}
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ControllerUtilsSpec extends PlaySpec with MockitoSugar with FakePBIKApplication {

  val testRequestBody: JsValue                   = Json.toJson(List.empty[String])
  implicit val request: Request[AnyContent]      = FakeRequest().withJsonBody(testRequestBody)
  implicit val formats: OFormat[PbikCredentials] = Json.format[PbikCredentials]

  val year: Int                    = 2014
  val employer_number_code: String = "Mock-Employer-Number-Code"
  val paye_scheme_type: Int        = 1000
  val mockedBaseUrl: String        = "baseUrl"
  val urlExtension: String         = "urlExtension"

  val mockCredentials: PbikCredentials = PbikCredentials(1, 2, 3, "aoReference", "payeSchemeOperatorName")

  def responseStatus200WithErrorBody(body: String): HttpResponse = HttpResponse(OK, body)

  val responseStatus200: HttpResponse = HttpResponse(OK, "Body of response with status 200")
  val responseStatus404: HttpResponse = HttpResponse(NOT_FOUND, "Body of response with status 404")

  def mockControllerUtils: ControllerUtils = app.injector.instanceOf[ControllerUtils]

  val mockTierConnector: HmrcTierConnectorWrapped = mock[HmrcTierConnectorWrapped]
  when(mockTierConnector.retrieveDataGet(anyString)(any[HeaderCarrier]))
    .thenReturn(Future.successful(HttpResponse(OK, Json.toJson(mockCredentials).toString())))

  "The valid Credential check" should {
    "Return valid credentials from NPS" in {
      val result = await(
        mockControllerUtils
          .retrieveCredentialsFromNPS(mockTierConnector, year, employer_number_code, paye_scheme_type)
      )
      result must be(mockCredentials)
    }
  }

  "The status to response map" should {
    "Successfully return a response with status 200" in {
      val result = mockControllerUtils.generateResultBasedOnStatus(Future(responseStatus200))
      contentAsString(result) must be("Body of response with status 200")
    }

    "Successfully return a response with status 200 but expected error inside" in {
      val failedResponse =
        "{\"message\":\"Internal Server Error\",\"statusCode\":500,\"appStatusMessage\":\";63082\",\"requestUri\":\"\"}"

      val result =
        mockControllerUtils.generateResultBasedOnStatus(Future(responseStatus200WithErrorBody(failedResponse)))
      status(result)          must be(OK)
      contentAsString(result) must be("[]")
    }

    "Successfully return a response with status 200 but unexpected error inside" in {
      val failedResponse =
        "{\"message\":\"Internal Server Error\",\"statusCode\":500,\"appStatusMessage\":\";1234567890\",\"requestUri\":\"\"}"

      val result =
        mockControllerUtils.generateResultBasedOnStatus(Future(responseStatus200WithErrorBody(failedResponse)))
      status(result)          must be(OK)
      contentAsString(result) must be("{\"errorCode\":\"1234567890\"}")
    }
  }

  "The status to response map" should {
    "Successfully return a response with status 404" in {
      val result = mockControllerUtils.generateResultBasedOnStatus(Future(responseStatus404))
      contentAsString(result) must be("Body of response with status 404")

    }
  }

  "The controller utils " should {
    "return valid credentials when a valid empref is supplied " in {
      val result = await(mockControllerUtils.retrieveNPSCredentials(mockTierConnector, 2015, "123/TEST1"))
      result.aoReference mustBe "aoReference"
    }
  }

  "The controller utils " should {
    "return a tuple splitting the empref correctly into 2 parts " in {
      val result = mockControllerUtils.extractEmployerRefParts("123/TEST1")
      result._1 mustBe "TEST1"
      result._2 mustBe 123
    }
  }

  "The controller utils " should {
    "return valid credentials when parsing a marshalled Pbikcredentials object from json " in {
      val result = await(mockControllerUtils.retrieveCredentialsFromNPS(mockTierConnector, 2015, "TEST1", 123))
      result.aoReference mustBe "aoReference"
    }
  }

  "The controller utils" should {
    "return error code when upstream error 63092" in {
      val failedResponse =
        "{\"message\":\"Internal Server Error\",\"statusCode\":500,\"appStatusMessage\":\";63092\",\"requestUri\":\"\"}"
      val result         = mockControllerUtils.extractUpstreamError(failedResponse)
      result must be("63092")
    }

    "return error code when upstream error 64989" in {
      val failedResponse =
        "{\"message\":\"Internal Server Error\",\"statusCode\":500,\"appStatusMessage\":\";64989\",\"requestUri\":\"\"}"
      val result         = mockControllerUtils.extractUpstreamError(failedResponse)
      result must be("64989")
    }

    "return default error code when upstream error is empty" in {
      val failedResponse =
        "{\"message\":\"Internal Server Error\",\"statusCode\":500,\"appStatusMessage\":\";Invalid version number\",\"requestUri\":\"\"}"
      val result         = mockControllerUtils.extractUpstreamError(failedResponse)
      result must be("")
    }

    "return default error code when upstream error not found" in {
      val failedResponse =
        "{\"message\":\"Internal Server Error\"}"
      val result         = mockControllerUtils.extractUpstreamError(failedResponse)
      result must be("10001")
    }

  }

  "When NPS returns response " should {
    "with headers return the ETAG and TXID headers" in {
      implicit val request: Request[AnyContent] = FakeRequest()
        .withHeaders(HeaderTags.ETAG -> "10", HeaderTags.X_TXID -> "1")

      val result                                = mockControllerUtils.getNPSMutatorSessionHeader(request)
      result must be(Map(HeaderTags.ETAG -> "10", HeaderTags.X_TXID -> "1"))
    }

    "with headers return the ETAG and no TXID headers" in {
      implicit val request: Request[AnyContent] = FakeRequest()
        .withHeaders(HeaderTags.ETAG -> "10")

      val result                                = mockControllerUtils.getNPSMutatorSessionHeader(request)
      result must be(Map(HeaderTags.ETAG -> "10", HeaderTags.X_TXID -> HeaderTags.X_TXID_DEFAULT_VALUE))
    }

    "return None when there is no ETAG value" in {
      val npsRequestBody                        = Json.toJson(List.empty[String])
      implicit val request: Request[AnyContent] = FakeRequest()
        .withJsonBody(npsRequestBody)
        .withHeaders()

      val result = mockControllerUtils.getNPSMutatorSessionHeader(request)
      result must be(empty)
    }
  }

  ".mapResponseToResult" when {

    "the response status is 200" should {
      "return an OK result with the response body and default headers" in {
        val response        = HttpResponse(OK, "response body")
        val expectedHeaders = Map(
          HeaderTags.ETAG   -> HeaderTags.ETAG_DEFAULT_VALUE,
          HeaderTags.X_TXID -> HeaderTags.X_TXID_DEFAULT_VALUE
        )

        val result = mockControllerUtils.mapResponseToResult(Future.successful(response))

        status(result) mustBe OK
        contentAsString(result) mustBe "response body"
        headers(result) mustBe expectedHeaders
      }

      "return an OK result with the response body and headers" in {
        val expectedHeaders        = Map(
          HeaderTags.ETAG   -> "10",
          HeaderTags.X_TXID -> "1"
        )
        val inputHeaders           = expectedHeaders.map { case (k, v) => k -> Seq(v) }
        val response: HttpResponse = HttpResponse(OK, "response body", inputHeaders)

        val result = mockControllerUtils.mapResponseToResult(Future.successful(response))

        status(result) mustBe OK
        contentAsString(result) mustBe "response body"
        headers(result) mustBe expectedHeaders
      }
    }

    "the response status is 400" should {
      "return a BadRequest result with the response non json body" in {
        val response = HttpResponse(BAD_REQUEST, "response body")

        val result = mockControllerUtils.mapResponseToResult(Future.successful(response))

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe Json.toJson(PbikError(s"$BAD_REQUEST.xxx")).toString()
      }

      "return a BadRequest result with the response is json body but not NPSError" in {
        val response = HttpResponse(BAD_REQUEST, """{"key": "value"}""")

        val result = mockControllerUtils.mapResponseToResult(Future.successful(response))

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe Json.toJson(PbikError(s"$BAD_REQUEST.xxx")).toString()
      }

      "return a BadRequest result with the response is NPSError json body" in {
        val response = HttpResponse(BAD_REQUEST, Json.toJson(NPSErrors(List(NPSError("reason", "code")))).toString())

        val result = mockControllerUtils.mapResponseToResult(Future.successful(response))

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe Json.toJson(PbikError("code")).toString()
      }
    }

  }
}
