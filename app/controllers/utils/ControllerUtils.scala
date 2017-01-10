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

package controllers.utils

import connectors.HmrcTierConnectorWrapped
import models.{EiLPerson, PbikError, HeaderTags, PbikCredentials}
import play.api.Logger
import play.api.i18n.Messages
import play.api.libs.json
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.{HttpGet, HttpPost, HttpResponse, NotFoundException}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import java.net.URLDecoder
import play.api.libs.functional.syntax._
import play.api.libs.json._

trait ControllerUtils extends URIInformation {
  val controllerUtils = new ControllerUtilsWrapped()
}

class ControllerUtilsWrapped() extends URIInformation {

  val credentialsId: String = "pbik-credentials-id"
  private val appStatusMessageRegex = "[0-9]+"
  val DEFAULT_ERROR = "10001"

    /**
   * generates a URL based on the credentials returned from NPS
   * @param credentials
   * @param year
   * @param baseUrl
   * @param urlExtension
   * @return
   */
  def generateURLBasedOnCredentials(credentials: PbikCredentials, year: Int, baseUrl: String, urlExtension: String): String = {
    val paye_scheme_type = credentials.payeSchemeType
    val employer_number = credentials.employerNumber
    val paye_seq_number = credentials.payeSequenceNumber
    s"$baseUrl/$year/$paye_scheme_type/$employer_number/$paye_seq_number/$urlExtension"
  }

  def extractUpstreamError(message:String)(implicit request: Request[AnyContent]):String = {
    val startindex:Int = message.indexOf("appStatusMessage")
    val endindex:Int = message.indexOf(",", startindex)
    if ( startindex >= 0 && endindex > startindex ) {
      val appStatusMessageSegment = message.substring(startindex, endindex)
      Logger.info("An NPS error code has been detected " + appStatusMessageSegment)
      val errorCode = appStatusMessageRegex.r.findAllIn(appStatusMessageSegment).mkString

      errorCode
    } else {
      DEFAULT_ERROR
    }
  }

  /**
   * generates a play http Result based on the status of the WSResponse
   * @param wsResponse
   * @return
   */
  def generateResultBasedOnStatus(wsResponse: Future[HttpResponse])(implicit request: Request[AnyContent], hc: HeaderCarrier, formats: json.Reads[Map[String, String]]): Future[Result] = {
    wsResponse.map {
      response => response.status match {
       case 200 => {
          response.body.contains("appStatusMessage") match {
            case true => {
              Logger.warn("GenerateResultBasedOnStatus Response Failed status:" + response.status + " json:" + " body:" + response.body )

              val msgValue = extractUpstreamError(response.body)

              val error = PbikError(msgValue)
              if(error.errorCode == "63082") Ok(Json.toJson(List[EiLPerson]()))
              else new Status(response.status)(Json.toJson(error))
            }
            case _ => {
              // TODO - why does response.header("eTag") return Null when the Option should.. but Tests fail without it
              val headers:Map[String, String] = if ( response.header(HeaderTags.ETAG) != null ) {
                Map(HeaderTags.ETAG -> response.header(HeaderTags.ETAG).getOrElse("0"), HeaderTags.X_TXID -> response.header(HeaderTags.X_TXID).getOrElse("1") )
              } else {
                Map(("",""))
              }

              Ok(response.body).withHeaders(headers.toSeq: _*)
            }
          }
        }
        case _ => {
          Logger.warn("GenerateResultBasedOnStatus Response Failed status:" + response.status + " json:" + " body:" + response.body + " request:" + request.body.asText)

          new Status(response.status)(response.body)
        }
      }
    }
  }

  def createCompositeKey(employer_code: String, paye_scheme_type: Int) = employer_code+"-"+paye_scheme_type

  def retrieveNPSCredentials(tierConnector: HmrcTierConnectorWrapped,year: Int, empRef:String)(implicit request: Request[AnyContent], hc: HeaderCarrier, formats: json.Format[PbikCredentials]): Future[PbikCredentials] = {

    val keyparts = extractEmployerRefParts(empRef)
    retrieveCrendtialsFromNPS(tierConnector, year, keyparts._1, keyparts._2 )

  }

  def extractEmployerRefParts(empRef:String):(String, Int) = {
    val employerReferenceString = decode(empRef)
    val tokens = employerReferenceString.split("/")
    val employer_number = tokens(0).toInt
    val paye_scheme_type = tokens(1)
    (paye_scheme_type,employer_number)
  }


  def retrieveCrendtialsFromNPS(tierConnector: HmrcTierConnectorWrapped, year: Int, employer_code: String, paye_scheme_type: Int)(implicit request: Request[AnyContent], hc: HeaderCarrier, formats: json.Format[PbikCredentials]): Future[PbikCredentials] = {

    tierConnector.retrieveDataGet(s"$baseURL/$year/$employer_code/$paye_scheme_type")(hc) map {
      result: HttpResponse => {
        result.json.validate[PbikCredentials].asOpt.get
      }
    }
  }

  def getNPSMutatorSessionHeader(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Option[Map[String, String]]] = {
    val pbikHeaders = if(request.headers.get(HeaderTags.ETAG).isDefined) {
      Some(Map(
        (HeaderTags.ETAG, request.headers.get(HeaderTags.ETAG).getOrElse("0")),
        (HeaderTags.X_TXID, request.headers.get(HeaderTags.X_TXID).getOrElse("1"))
      ))
    } else {None}

    Future(pbikHeaders)
  }

  def decode(encodedEmpRef: String) = URLDecoder.decode(encodedEmpRef, "UTF-8")

}
