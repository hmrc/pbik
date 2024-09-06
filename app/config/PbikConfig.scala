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

import models.v1.PbikCredentials
import play.api.Configuration

import java.util.Base64
import javax.inject.Inject

class PbikConfig @Inject() (conf: Configuration) {

  private val clientIdV1: String = conf.get[String]("microservice.services.nps.hip.clientId")
  private val secretV1: String   = conf.get[String]("microservice.services.nps.hip.secret")
  def authorizationToken: String = Base64.getEncoder.encodeToString(s"$clientIdV1:$secretV1".getBytes("UTF-8"))

  val serviceOriginatorIdKeyV1: String = conf.get[String]("microservice.services.nps.hip.originatoridkey")
  val serviceOriginatorIdV1: String    = conf.get[String]("microservice.services.nps.hip.originatoridvalue")

  private def getServiceUrl(serviceName: String): String = {
    val host     = conf.get[String](s"microservice.services.$serviceName.host")
    val port     = conf.get[String](s"microservice.services.$serviceName.port")
    val protocol = conf.get[String](s"microservice.services.$serviceName.protocol")

    s"$protocol://$host:$port"
  }

  private val baseNPSJsonURL: String = s"${getServiceUrl("nps.hip")}/paye/employer"

  def getRegisteredBenefitsPath(credentials: PbikCredentials, year: Int): String =
    s"$baseNPSJsonURL/${credentials.employmentIdentifier}/payrolled-benefits-in-kind/$year"

  def putRegisteredBenefitsPath(credentials: PbikCredentials, year: Int): String =
    s"$baseNPSJsonURL/${credentials.employmentIdentifier}/payrolled-benefits-in-kind/$year"

  def getAllBenefitsPath(year: Int): String = s"$baseNPSJsonURL/payrolled-benefits-in-kind/current/$year"

  def getEmployerDetailsPath(taxDistrictNumber: String, payeNumber: String) =
    s"$baseNPSJsonURL/paye-scheme/$taxDistrictNumber/$payeNumber/summary"

  def getAllExcludedPeopleForABenefitPath(credentials: PbikCredentials, year: Int, ibdtype: String) =
    s"$baseNPSJsonURL/${credentials.employmentIdentifier}/payrolled-benefits-in-kind/exclusion-list/$year/$ibdtype"

  def putExcludedPeopleForABenefitPath(credentials: PbikCredentials, year: Int) =
    s"$baseNPSJsonURL/${credentials.employmentIdentifier}/payrolled-benefits-in-kind/exclusion-list/$year"

  def removeExcludedPeopleForABenefitPath(credentials: PbikCredentials, year: Int) =
    s"$baseNPSJsonURL/${credentials.employmentIdentifier}/payrolled-benefits-in-kind/exclusion-list/$year"

}
