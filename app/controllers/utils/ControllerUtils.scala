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

import config.PbikConfig
import connectors.HmrcTierConnectorWrapped
import models.v1.NPSError
import models.{HeaderTags, PbikCredentials, PbikError}
import play.api.Logging
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import java.net.URLDecoder
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class ControllerUtils @Inject() (pbikConfig: PbikConfig)(implicit val executionContext: ExecutionContext)
    extends Logging {

  //new error handlers
  private def responseToNPSError(response: HttpResponse) = {
    val defaultCode = s"${response.status}.xxx"

    Try(response.json) match {
      case Failure(_)    =>
        logger.error(
          s"[GatewayNPSController][responseToNPSError] Not json body defaulting to $defaultCode"
        )
        PbikError(defaultCode)
      case Success(json) =>
        json.asOpt[NPSError] match {
          case Some(error) =>
            logger.error(
              s"[GatewayNPSController][responseToNPSError] NPS Error code: ${error.code} message: ${error.reason}"
            )
            PbikError(error.code)
          case None        =>
            logger.error(
              s"[GatewayNPSController][responseToNPSError] Failed to convert to NPS error code defaulting to $defaultCode"
            )
            PbikError(defaultCode)
        }
    }
  }

  def mapResponseToResult(wsResponse: Future[HttpResponse]): Future[Result] = wsResponse.map { response =>
    response.status match {
      case OK =>
        val headers: Map[String, String] = Map(
          HeaderTags.ETAG   -> response.header(HeaderTags.ETAG).getOrElse("0"),
          HeaderTags.X_TXID -> response.header(HeaderTags.X_TXID).getOrElse("1")
        )

        Ok(response.body).withHeaders(headers.toSeq: _*)
      case _  =>
        val pbikError: PbikError = responseToNPSError(response)
        new Status(response.status)(Json.toJson(pbikError))
    }
  }

  //old error handlers
  private val appStatusMessageRegex = "[0-9]+"
  private val defaultError          = "10001"

  def extractUpstreamError(message: String): String = {
    val startindex: Int = message.indexOf("appStatusMessage")
    val endindex: Int   = message.indexOf(",", startindex)
    if (startindex >= 0 && endindex > startindex) {
      val appStatusMessageSegment = message.substring(startindex, endindex)
      logger.error(
        s"[ControllerUtils][extractUpstreamError] An NPS error code has been detected $appStatusMessageSegment"
      )
      appStatusMessageRegex.r.findAllIn(appStatusMessageSegment).mkString
    } else {
      defaultError
    }
  }

  def generateResultBasedOnStatus(
    wsResponse: Future[HttpResponse]
  )(implicit request: Request[AnyContent]): Future[Result] =
    wsResponse.map { response =>
      response.status match {
        case OK =>
          if (response.body.contains("appStatusMessage")) {
            logger.warn(
              s"[ControllerUtils][generateResultBasedOnStatus] GenerateResultBasedOnStatus Response Failed status: ${response.status}"
            )
            val msgValue = extractUpstreamError(response.body)
            val error    = PbikError(msgValue)
            //TODO why we are returning error as 200, in both branches why in second we dont turn it into 500 or 400?
            if (error.errorCode == "63082") {
              Ok(Json.toJson(List[String]())) // defaults to empty person list but empty string will work as well
            } else {
              new Status(response.status)(Json.toJson(error))
            }
          } else {
            val headers: Map[String, String] = Map(
              HeaderTags.ETAG   -> response.header(HeaderTags.ETAG).getOrElse("0"),
              HeaderTags.X_TXID -> response.header(HeaderTags.X_TXID).getOrElse("1")
            )

            Ok(response.body).withHeaders(headers.toSeq: _*)
          }
        case _  =>
          logger.warn(
            s"[ControllerUtils][generateResultBasedOnStatus] GenerateResultBasedOnStatus Response Failed status:${response.status}" +
              s" json:body:${response.body} request:${request.body.asText}"
          )
          new Status(response.status)(response.body)
      }
    }

  def retrieveNPSCredentials(tierConnector: HmrcTierConnectorWrapped, year: Int, empRef: String)(implicit
    hc: HeaderCarrier
  ): Future[PbikCredentials] = {
    val tuple = extractEmployerRefParts(empRef)
    retrieveCredentialsFromNPS(tierConnector, year, tuple._1, tuple._2)
  }

  def extractEmployerRefParts(empRef: String): (String, Int) = {
    val employerReferenceString = decode(empRef)
    val tokens                  = employerReferenceString.split("/")
    val employer_number         = tokens(0).toInt
    val paye_scheme_type        = tokens(1)
    (paye_scheme_type, employer_number)
  }

  def retrieveCredentialsFromNPS(
    tierConnector: HmrcTierConnectorWrapped,
    year: Int,
    employer_code: String,
    paye_scheme_type: Int
  )(implicit hc: HeaderCarrier): Future[PbikCredentials] =
    tierConnector.retrieveDataGet(s"${pbikConfig.baseURL}/$year/$employer_code/$paye_scheme_type")(hc) map {
      result: HttpResponse =>
        result.json.validate[PbikCredentials].asOpt.get
    }

  def getNPSMutatorSessionHeader(implicit request: Request[_]): Map[String, String] =
    request.headers.get(HeaderTags.ETAG) match {
      case Some(etag) =>
        val txid = request.headers.get(HeaderTags.X_TXID).getOrElse(HeaderTags.X_TXID_DEFAULT_VALUE)
        HeaderTags.createResponseHeaders(etag, txid)
      case None       =>
        Map[String, String]()
    }

  private def decode(encodedEmpRef: String): String = URLDecoder.decode(encodedEmpRef, "UTF-8")

}
