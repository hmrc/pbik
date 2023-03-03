/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.{Configuration, Logging}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import java.util.UUID.randomUUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

class HmrcTierConnectorWrapped @Inject() (val http: HttpClient, configuration: Configuration)(implicit
  val executionContext: ExecutionContext
) extends Logging {

  val serviceOriginatorIdKey: String = configuration.get[String]("microservice.services.nps.originatoridkey")
  val serviceOriginatorId: String    = configuration.get[String]("microservice.services.nps.originatoridvalue")
  val CORRELATION_HEADER: String     = "CorrelationId"
  val requestIdPattern: Regex        = """.*([A-Za-z0-9]{8}-[A-Za-z0-9]{4}-[A-Za-z0-9]{4}-[A-Za-z0-9]{4}).*""".r

  private def buildHeaders(correlationId: String): Seq[(String, String)] =
    Seq(
      serviceOriginatorIdKey -> serviceOriginatorId,
      CORRELATION_HEADER     -> correlationId
    )

  def generateNewUUID: String = randomUUID.toString

  private[connectors] def getCorrelationId(hc: HeaderCarrier): String =
    hc.requestId match {
      case Some(requestId) =>
        requestId.value match {
          case requestIdPattern(prefix) =>
            val twelveRandomDigits = generateNewUUID.takeRight(12)
            prefix + "-" + twelveRandomDigits
          case _                        => generateNewUUID
        }
      case _               => generateNewUUID
    }

  def retrieveDataGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val correlationId = getCorrelationId(hc)
    http.GET(url, headers = buildHeaders(correlationId)).recover { case ex =>
      logger.error(
        s"[HmrcTierConnectorWrapped][retrieveDataGet] an execption occured ${ex.getMessage}, when calling $url",
        ex
      )
      HttpResponse(Status.OK, json = Json.toJson(ex.getMessage), Map.empty)
    }
  }

  def retrieveDataPost(headers: Map[String, String], url: String, requestBody: JsValue)(implicit
    hc: HeaderCarrier
  ): Future[HttpResponse] = {
    val correlationId = getCorrelationId(hc)
    http.POST(url, requestBody, headers = buildHeaders(correlationId) ++ headers.toSeq).recover { case ex =>
      logger.error(
        s"[HmrcTierConnectorWrapped][retrieveDataPost] an execption occured ${ex.getMessage}, when calling $url",
        ex
      )
      HttpResponse(Status.OK, json = Json.toJson(ex.getMessage), Map.empty)
    }
  }
}
