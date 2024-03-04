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

package helper

import controllers.actions.MinimalAuthAction
import models.Bik
import models.v1.IabdType
import org.scalatest.TestSuite
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import java.util.UUID

trait FakePBIKApplication extends GuiceOneAppPerSuite {

  this: TestSuite =>

  val config: Map[String, Any] = Map(
    "application.secret"                          -> "Its secret",
    "csrf.sign.tokens"                            -> false,
    "microservice.services.contact-frontend.host" -> "localhost",
    "microservice.services.contact-frontend.port" -> "9250",
    "sessionId"                                   -> "a-session-id"
  )

  private val QUERY_STATUS: Int = 10

  val biks: Seq[Bik] = Seq(
    Bik(IabdType.MedicalInsurance.id.toString, QUERY_STATUS),
    Bik(IabdType.CarBenefit.id.toString, QUERY_STATUS),
    Bik(IabdType.OtherItems.id.toString, QUERY_STATUS)
  )

  val sampleBikJson: String = Json.toJson(biks).toString()

  def mockrequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withSession(
      SessionKeys.sessionId       -> s"session-${UUID.randomUUID}",
      SessionKeys.authToken       -> "RANDOMTOKEN",
      SessionKeys.sensitiveUserId -> "test-user-id"
    )

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override lazy val fakeApplication: Application = GuiceApplicationBuilder()
    .configure(config)
    .overrides(bind(classOf[MinimalAuthAction]).to(classOf[TestMinimalAuthAction]))
    .build()

}
