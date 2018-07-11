/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.play.config.ServicesConfig

import scala.util.Try

object HmrcTierConnectorWrapped extends HmrcTierConnectorWrapped {

}

trait HmrcTierConnectorWrapped extends ServicesConfig {

  val serviceOriginatorIdKey = Try{getConfString("nps.originatoridkey", "")}.getOrElse("")
  val serviceOriginatorId = Try{getConfString("nps.originatoridvalue", "")}.getOrElse("")

  def retrieveDataGet(url: String)(hc:HeaderCarrier): Future[HttpResponse] = {
    implicit val hcextra = hc.withExtraHeaders(serviceOriginatorIdKey -> serviceOriginatorId)
    HmrcTierConnector.http.GET(url).recover{
      case e => {
        Logger.warn("retrieveDataGet Failed, " + e)
        HttpResponse(200, Some(Json.toJson(e.getMessage)))
      }
    }
 }

  def retrieveDataPost(headers: Map[String,String], url: String, requestBody: JsValue)(hac:HeaderCarrier): Future[HttpResponse] = {
    implicit val hcextra = hac.withExtraHeaders(headers.toSeq: _*).withExtraHeaders(serviceOriginatorIdKey -> serviceOriginatorId)
    HmrcTierConnector.http.POST(url,requestBody).recover{
      case e => {
        Logger.warn("retrieveDataPost Failed, " + e)
        HttpResponse(200, Some(Json.toJson(e.getMessage)))
      }
    }
  }

}
