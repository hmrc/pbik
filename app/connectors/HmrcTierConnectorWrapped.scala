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

import javax.inject.Inject
import play.api.libs.json.{JsValue, Json}
import play.api.{Configuration, Logger}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HmrcTierConnectorWrapped @Inject()(val http: HttpClient, configuration: Configuration) {

  val serviceOriginatorIdKey: String = configuration.get[String]("microservice.services.nps.originatoridkey")
  val serviceOriginatorId: String = configuration.get[String]("microservice.services.nps.originatoridvalue")

  def retrieveDataGet(url: String)(hc: HeaderCarrier): Future[HttpResponse] = {
    implicit val hcextra: HeaderCarrier = hc.withExtraHeaders(serviceOriginatorIdKey -> serviceOriginatorId)
    http.GET(url).recover {
      case ex => {
        Logger.error(
          s"[HmrcTierConnectorWrapped][retrieveDataGet] an execption occured ${ex.getMessage}, when calling $url",
          ex)
        HttpResponse(200, Some(Json.toJson(ex.getMessage)))
      }
    }
  }

  def retrieveDataPost(headers: Map[String, String], url: String, requestBody: JsValue)(
    hac: HeaderCarrier): Future[HttpResponse] = {
    implicit val hcextra: HeaderCarrier =
      hac.withExtraHeaders(headers.toSeq: _*).withExtraHeaders(serviceOriginatorIdKey -> serviceOriginatorId)
    http.POST(url, requestBody).recover {
      case ex => {
        Logger.error(
          s"[HmrcTierConnectorWrapped][retrieveDataPost] an execption occured ${ex.getMessage}, when calling $url",
          ex)
        HttpResponse(200, Some(Json.toJson(ex.getMessage)))
      }
    }
  }
}
