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
import models.PbikCredentials
import models.v1.BenefitListUpdateRequest
import play.api.Logging
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import java.util.UUID.randomUUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

class HmrcTierConnectorWrapped @Inject() (
  val http: HttpClientV2,
  pbikConfig: PbikConfig,
  val controllerUtils: ControllerUtils
)(implicit
  val executionContext: ExecutionContext
) extends Logging {

  private val CORRELATION_HEADER: String   = "CorrelationId"
  private val AUTHORIZATION_HEADER: String = "Authorization"
  private val requestIdPattern: Regex      = """.*([A-Za-z0-9]{8}-[A-Za-z0-9]{4}-[A-Za-z0-9]{4}-[A-Za-z0-9]{4}).*""".r

  private def buildHeaders(correlationId: String): Seq[(String, String)] =
    Seq(
      pbikConfig.serviceOriginatorIdKey -> pbikConfig.serviceOriginatorId,
      CORRELATION_HEADER                -> correlationId
    )

  private def buildHeadersV1(correlationId: String): Seq[(String, String)] =
    Seq(
      pbikConfig.serviceOriginatorIdKeyV1 -> pbikConfig.serviceOriginatorIdV1,
      CORRELATION_HEADER                  -> correlationId,
      AUTHORIZATION_HEADER                -> s"Basic ${pbikConfig.authorizationToken}"
    )

  def generateNewUUID: String = randomUUID.toString

  def getCorrelationId(hc: HeaderCarrier): String =
    hc.requestId match {
      case Some(requestId) =>
        requestId.value match {
          case requestIdPattern(prefix) =>
            val lastTwelveChars = generateNewUUID.takeRight(12)
            prefix + "-" + lastTwelveChars
          case _                        => generateNewUUID
        }
      case _               => generateNewUUID
    }

  def retrieveDataGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val correlationId = getCorrelationId(hc)
    http
      .get(url"$url")
      .setHeader(buildHeaders(correlationId): _*)
      .execute[HttpResponse]
      .recover { case ex =>
        logger.error(
          s"[HmrcTierConnectorWrapped][retrieveDataGet] an exception occurred ${ex.getMessage}, when calling $url",
          ex
        )
        HttpResponse(Status.OK, json = Json.toJson(ex.getMessage), Map.empty)
      }
  }

  def retrieveDataPost(url: String, requestBody: JsValue)(implicit
    hc: HeaderCarrier,
    request: Request[_]
  ): Future[HttpResponse] = {
    val correlationId = getCorrelationId(hc)
    val npsHeaders    = controllerUtils.getNPSMutatorSessionHeader
    val allHeaders    = buildHeaders(correlationId) ++ npsHeaders.toSeq
    http
      .post(url"$url")
      .setHeader(allHeaders: _*)
      .withBody(requestBody)
      .execute[HttpResponse]
      .recover { case ex =>
        logger.error(
          s"[HmrcTierConnectorWrapped][retrieveDataPost] an exception occurred ${ex.getMessage}, when calling $url",
          ex
        )
        HttpResponse(Status.OK, json = Json.toJson(ex.getMessage), Map.empty)
      }
  }

  // new v1 api

  def getRegisteredBenefits(credentials: PbikCredentials, year: Int)(implicit
    hc: HeaderCarrier
  ): Future[HttpResponse] = {
    val url           = pbikConfig.getRegisteredBenefitsPath(credentials, year)
    val correlationId = getCorrelationId(hc)
    http
      .get(url"$url")
      .setHeader(buildHeadersV1(correlationId): _*)
      .execute[HttpResponse]
      .recover { case ex =>
        logger.error(
          s"[HmrcTierConnectorWrapped][getRegisteredBenefits] an exception occurred ${ex.getMessage}, when calling $url",
          ex
        )
        HttpResponse(Status.OK, json = Json.toJson(ex.getMessage), Map.empty)
      }
  }

  def updateBenefitTypes(url: String, bikToUpdateRequest: BenefitListUpdateRequest)(implicit
    hc: HeaderCarrier,
    request: Request[_]
  ): Future[HttpResponse] = {
    val requestBody     = Json.toJson(bikToUpdateRequest)
    val correlationId   = getCorrelationId(hc)
    val npsHeaders      = controllerUtils.getNPSMutatorSessionHeader
    val contentTypeJson = "Content-Type" -> "application/json"
    val allHeaders      = buildHeadersV1(correlationId) ++ npsHeaders.toSeq :+ contentTypeJson
    http
      .put(url"$url")
      .setHeader(allHeaders: _*)
      .withBody(requestBody)
      .execute[HttpResponse]
      .recover { case ex =>
        logger.error(
          s"[HmrcTierConnectorWrapped][updateBenefitTypes] an exception occurred ${ex.getMessage}, when calling $url",
          ex
        )
        HttpResponse(Status.OK, json = Json.toJson(ex.getMessage), Map.empty)
      }
  }
}
