/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.{Configuration, Logging}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HmrcTierConnectorWrapped @Inject()(val http: HttpClient, configuration: Configuration) extends Logging {

  val serviceOriginatorIdKey: String = configuration.get[String]("microservice.services.nps.originatoridkey")
  val serviceOriginatorId: String = configuration.get[String]("microservice.services.nps.originatoridvalue")

  def retrieveDataGet(url: String)(hc: HeaderCarrier): Future[HttpResponse] = {
    implicit val hcextra: HeaderCarrier = hc.withExtraHeaders(serviceOriginatorIdKey -> serviceOriginatorId)
    http.GET(url).recover {
      case ex =>
        logger.error(
          s"[HmrcTierConnectorWrapped][retrieveDataGet] an execption occured ${ex.getMessage}, when calling $url",
          ex)
        HttpResponse(Status.OK, json = Json.toJson(ex.getMessage), Map.empty)
    }
  }

  def retrieveDataPost(headers: Map[String, String], url: String, requestBody: JsValue)(
    hac: HeaderCarrier): Future[HttpResponse] = {
    implicit val hcextra: HeaderCarrier =
      hac.withExtraHeaders(headers.toSeq: _*).withExtraHeaders(serviceOriginatorIdKey -> serviceOriginatorId)
    http.POST(url, requestBody).recover {
      case ex =>
        logger.error(
          s"[HmrcTierConnectorWrapped][retrieveDataPost] an execption occured ${ex.getMessage}, when calling $url",
          ex)
        HttpResponse(Status.OK, json = Json.toJson(ex.getMessage), Map.empty)
    }
  }
}
