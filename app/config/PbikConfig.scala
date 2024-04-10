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

package config

import models.PbikCredentials
import play.api.Configuration

import javax.inject.Inject

class PbikConfig @Inject() (conf: Configuration) {

  private def getServiceUrl(serviceName: String): String = {
    val host     = conf.get[String](s"microservice.services.$serviceName.host")
    val port     = conf.get[String](s"microservice.services.$serviceName.port")
    val protocol = conf.get[String](s"microservice.services.$serviceName.protocol")

    s"$protocol://$host:$port"
  }

  val serviceOriginatorIdKey: String = conf.get[String]("microservice.services.nps.originatoridkey")
  val serviceOriginatorId: String    = conf.get[String]("microservice.services.nps.originatoridvalue")

  val serviceOriginatorIdKeyV1: String = conf.get[String]("microservice.services.nps.hip.originatoridkey")
  val serviceOriginatorIdV1: String    = conf.get[String]("microservice.services.nps.hip.originatoridvalue")

  val getBenefitTypesPath: String = "getbenefittypes"
  val exclusionPath: String       = "exclusion"
  val addExclusionPath: String    = "exclusion/update"
  val removeExclusionPath: String = "exclusion/remove"

  val baseURL: String = s"${getServiceUrl("nps")}/nps-hod-service/services/nps/employer/payroll-bik"

  private val baseNPSJsonURL: String = s"${getServiceUrl("nps.hip")}/nps-json-service/nps/v1/api/employer"

  def getRegisteredBenefitsPath(credentials: PbikCredentials, year: Int): String =
    s"$baseNPSJsonURL/${credentials.employerNumber}/payrolled-benefits-in-kind/$year/${credentials.payeSchemeType}/${credentials.payeSequenceNumber}"

  def putRegisteredBenefitsPath(credentials: PbikCredentials, year: Int): String =
    s"$baseNPSJsonURL/${credentials.employerNumber}/payrolled-benefits-in-kind/$year/${credentials.payeSchemeType}/${credentials.payeSequenceNumber}"

}
