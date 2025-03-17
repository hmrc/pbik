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
import models.v1
import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import java.util.UUID.randomUUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

@Singleton
class NpsConnector @Inject() (http: HttpClientV2, pbikConfig: PbikConfig)(implicit ec: ExecutionContext)
    extends Logging {

  private val CORRELATION_HEADER: String   = "CorrelationId"
  private val AUTHORIZATION_HEADER: String = "Authorization"
  private val requestIdPattern: Regex      = """.*([A-Za-z0-9]{8}-[A-Za-z0-9]{4}-[A-Za-z0-9]{4}-[A-Za-z0-9]{4}).*""".r

  private def buildHeadersV1(implicit hc: HeaderCarrier): Seq[(String, String)] =
    Seq(
      pbikConfig.serviceOriginatorIdKeyV1 -> pbikConfig.serviceOriginatorIdV1,
      CORRELATION_HEADER                  -> getCorrelationId(hc),
      AUTHORIZATION_HEADER                -> s"Basic ${pbikConfig.authorizationToken}"
    )

  def generateNewUUID: String = randomUUID.toString

  def getCorrelationId(hc: HeaderCarrier): String =
    try {
      val candidateUUID = hc.requestId
        .map(_.value)
        .flatMap(rid => requestIdPattern.findFirstMatchIn(rid).map(_.group(1)))
        .map(prefix => s"$prefix-${generateNewUUID.takeRight(12)}")
        .getOrElse(generateNewUUID)

      // Validate the UUID by attempting to parse it
      // happened once that prefix had not uuid type format so generated cid was invalid
      java.util.UUID.fromString(candidateUUID)
      candidateUUID
    } catch {
      case _: Exception => generateNewUUID
    }

  def getRegisteredBenefits(credentials: v1.PbikCredentials, year: Int)(implicit
    hc: HeaderCarrier
  ): Future[HttpResponse] = {
    val fullUrl = pbikConfig.getRegisteredBenefitsPath(credentials, year)
    http
      .get(url"$fullUrl")
      .setHeader(buildHeadersV1: _*)
      .execute[HttpResponse]
  }

  def updateBenefitTypes(pbikCredentials: v1.PbikCredentials, taxYear: Int, requestBody: JsValue)(implicit
    hc: HeaderCarrier
  ): Future[HttpResponse] = {
    val fullUrl    = pbikConfig.putRegisteredBenefitsPath(pbikCredentials, taxYear)
    val allHeaders = buildHeadersV1
    http
      .put(url"$fullUrl")
      .setHeader(allHeaders: _*)
      .withBody(requestBody)
      .execute[HttpResponse]
  }

  def getBenefitTypes(year: Int)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val fullUrl = pbikConfig.getAllBenefitsPath(year)
    http
      .get(url"$fullUrl")
      .setHeader(buildHeadersV1: _*)
      .execute[HttpResponse]
  }

  def getPbikCredentials(taxDistrictNumber: String, payeNumber: String, year: Int)(implicit
    hc: HeaderCarrier
  ): Future[v1.PbikCredentials] = {
    val fullUrl = pbikConfig.getEmployerDetailsPath(taxDistrictNumber, payeNumber, year)
    http
      .get(url"$fullUrl")
      .setHeader(buildHeadersV1: _*)
      .execute[HttpResponse]
      .map { response =>
        // fail fast in case not expected body
        response.json.validate[v1.PbikCredentials] match {
          case JsSuccess(value, _) =>
            value
          case JsError(errors)     =>
            logger.error(s"[HmrcTierConnectorWrapped][getPbikCredentials] Invalid JSON: ${errors.mkString(", ")}")
            throw new IllegalArgumentException(s"Invalid JSON received from NPS, ${errors.mkString(", ")}")
        }
      }
  }

  def getAllExcludedPeopleForABenefit(pbikCredentials: v1.PbikCredentials, taxYear: Int, iabdType: String)(implicit
    hc: HeaderCarrier
  ): Future[HttpResponse] = {
    val fullUrl = pbikConfig.getAllExcludedPeopleForABenefitPath(pbikCredentials, taxYear, iabdType)
    http
      .get(url"$fullUrl")
      .setHeader(buildHeadersV1: _*)
      .execute[HttpResponse]
  }

  def updateExcludedPeopleForABenefit(pbikCredentials: v1.PbikCredentials, taxYear: Int, exclusions: JsValue)(implicit
    hc: HeaderCarrier
  ): Future[HttpResponse] = {
    val fullUrl = pbikConfig.postExcludedPeopleForABenefitPath(pbikCredentials, taxYear)
    http
      .post(url"$fullUrl")
      .setHeader(buildHeadersV1: _*)
      .withBody(exclusions)
      .execute[HttpResponse]
  }

  def removeExcludedPeopleForABenefit(
    pbikCredentials: v1.PbikCredentials,
    taxYear: Int,
    iabd: String,
    exclusions: JsValue
  )(implicit
    hc: HeaderCarrier
  ): Future[HttpResponse] = {
    val fullUrl = pbikConfig.removeExcludedPeopleForABenefitPath(pbikCredentials, taxYear, iabd)
    http
      .post(url"$fullUrl")
      .setHeader(buildHeadersV1: _*)
      .withBody(exclusions)
      .execute[HttpResponse]
  }

  def tracePeople(pbikCredentials: v1.PbikCredentials, taxYear: Int, body: JsValue)(implicit
    hc: HeaderCarrier
  ): Future[HttpResponse] = {
    val fullUrl = pbikConfig.postTracePerson(pbikCredentials, taxYear)
    http
      .post(url"$fullUrl")
      .setHeader(buildHeadersV1: _*)
      .withBody(body)
      .execute[HttpResponse]
  }

}
