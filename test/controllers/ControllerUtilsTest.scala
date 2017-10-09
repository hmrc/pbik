/*
 * Copyright 2017 HM Revenue & Customs
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

import connectors.{HmrcTierConnector, HmrcTierConnectorWrapped}
import controllers.utils.{ControllerUtils, ControllerUtilsWrapped, URIInformation}
import models.{HeaderTags, PbikCredentials}
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.test._
import play.api.test.Helpers._
import helper.MaterializerSupport
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.Application
import play.api.http.HttpEntity.Strict
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }

class ControllerUtilsTest extends PlaySpec with OneServerPerSuite  with MockitoSugar with FakePBIKApplication with MaterializerSupport {

  implicit lazy override val app: Application = new GuiceApplicationBuilder()
    .configure(config)
    .build()

  val testRequestBody = Json.toJson(List.empty[String])
  implicit val request: Request[AnyContent] = FakeRequest().withJsonBody(testRequestBody)
  implicit val formats = Json.format[PbikCredentials]
  //implicit val hc = new HeaderCarrier()

  val year = 2014
  val employer_number_code = "Mock-Employer-Number-Code"
  val paye_scheme_type = 1000
  val mockedBaseUrl="baseUrl"
  val urlExtension = "urlExtension"

  val mockCredentials = PbikCredentials(1, 2, 3, "aoReference", "payeSchemeOperatorName")
  class FakeResponse extends HttpResponse {
    override val allHeaders = Map[scala.Predef.String, scala.Seq[scala.Predef.String]]()
    override def status = 200
    override val json = Json.toJson(mockCredentials)
    override val body = Json.toJson(mockCredentials).toString()
  }
  //TODO refine when more info about statuses is available
  val mockWsResponseStatus200  = mock[HttpResponse]
  val mockWsResponseStatus404  = mock[HttpResponse]

  when(mockWsResponseStatus200.status).thenReturn(200)
  when(mockWsResponseStatus200.body).thenReturn("Body of response with status 200")
  when(mockWsResponseStatus404.status).thenReturn(404)
  when(mockWsResponseStatus404.body).thenReturn("Body of response with status 404")

  val mockHmrcTierConnectorWrapped = mock[HmrcTierConnectorWrapped]

  val mockWSResponse = mock[HttpResponse]
  when(mockHmrcTierConnectorWrapped.retrieveDataGet(anyString())(anyObject())).thenReturn(Future.successful(mockWSResponse))

  when(mockWSResponse.json) thenReturn Json.toJson(mockCredentials)

  trait MockURIInformation extends URIInformation {
    override lazy val baseURL = mockedBaseUrl
    override lazy val serviceUrl = mockedBaseUrl
  }

  def mockControllerUtils = new ControllerUtilsWrapped() with MockURIInformation {

  }

  val mockTierConnector = mock[HmrcTierConnectorWrapped]
  when(mockTierConnector.retrieveDataGet(anyString)(any[HeaderCarrier])).thenReturn(Future.successful(new FakeResponse))


  "The valid Credential check" should {
    "Return valid credentials from NPS" in {
      running(app) {
        val result = await(mockControllerUtils.retrieveCrendtialsFromNPS(mockHmrcTierConnectorWrapped, year, employer_number_code, paye_scheme_type))
        result must be(mockCredentials)
      }
    }
  }

  "The status to response map" should {
    "Successfully return a response with status 200" in {

      val result = await(mockControllerUtils.generateResultBasedOnStatus(Future{mockWsResponseStatus200}))
      result.body.asInstanceOf[Strict].data.utf8String must be("Body of response with status 200")

    }
  }

  "The status to response map" should {
    "Successfully return a response with status 404" in {

      val result = await(mockControllerUtils.generateResultBasedOnStatus(Future{mockWsResponseStatus404}))
      result.body.asInstanceOf[Strict].data.utf8String must be("Body of response with status 404")

    }
  }

  "The validation url generator" should {
    "Generate valid url" in {

      val result = mockControllerUtils.generateURLBasedOnCredentials(mockCredentials,year, mockedBaseUrl,urlExtension)
      result must be("baseUrl/2014/1/2/3/urlExtension")

    }
  }

  "The controller utils " should {
  "return valid credentials when a valid empref is supplied " in {
    val result = await(mockControllerUtils.retrieveNPSCredentials(mockTierConnector,2015,"123/TEST1"))
    assert(result.aoReference == "aoReference")
    }
  }

  "The controller utils " should {
    "return a tuple splitting the empref correctly into 2 parts " in {
      val result = mockControllerUtils.extractEmployerRefParts("123/TEST1")
      assert(result._1 == "TEST1")
      assert(result._2 == 123)
    }
  }

  "The controller utils " should {
    "return valid credentials when parsing a marshalled Pbikcredentials object from json " in {
      val result = await(mockControllerUtils.retrieveCrendtialsFromNPS(mockTierConnector,2015, "TEST1", 123))
      assert(result.aoReference == "aoReference")
    }
  }

  "The controller utils" should {
    "return error code when upstream error 63092" in {
      val failedResponse = "{\"message\":\"Internal Server Error\",\"statusCode\":500,\"appStatusMessage\":\";63092\",\"requestUri\":\"\"}"
      val result = mockControllerUtils.extractUpstreamError(failedResponse)
      result must be("63092")
    }

    "return error code when upstream error 64989" in {
      val failedResponse = "{\"message\":\"Internal Server Error\",\"statusCode\":500,\"appStatusMessage\":\";64989\",\"requestUri\":\"\"}"
      val result = mockControllerUtils.extractUpstreamError(failedResponse)
      result must be("64989")
    }

    "return default error code when upstream error not found" in {
      val failedResponse = "{\"message\":\"Internal Server Error\",\"statusCode\":500,\"appStatusMessage\":\";Invalid version number\",\"requestUri\":\"\"}"
      val result = mockControllerUtils.extractUpstreamError(failedResponse)
      result must be("")
    }

  }

  "When NPS returns response " should {
    "with headers return the ETAG and TXID headers" in {

      val npsRequestBody = Json.toJson(List.empty[String])
      implicit val request: Request[AnyContent] = FakeRequest().withJsonBody(npsRequestBody)
        .withHeaders(HeaderTags.ETAG -> "10", HeaderTags.X_TXID -> "1")

      val result = await(mockControllerUtils.getNPSMutatorSessionHeader(request, hc))
      result must be(Some(Map("ETag" -> "10", "X-TXID" -> "1")))
    }

    "return None when there is no ETAG value" in {

      val npsRequestBody = Json.toJson(List.empty[String])
      implicit val request: Request[AnyContent] = FakeRequest().withJsonBody(npsRequestBody)
        .withHeaders()

      val result = await(mockControllerUtils.getNPSMutatorSessionHeader(request, hc))
      result must be(None)
    }
  }

}
