/*
 * Copyright 2025 HM Revenue & Customs
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

import models.v1.PbikCredentials
import org.scalatest.TestSuite
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import java.util.UUID

trait FakePBIKApplication extends GuiceOneAppPerSuite { this: TestSuite =>

  val config: Map[String, Any] = Map(
    "metrics.jvm"      -> false,
    "metrics.enabled"  -> false,
    "auditing.enabled" -> false
  )

  def mockRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withSession(
      SessionKeys.sessionId       -> s"session-${UUID.randomUUID}",
      SessionKeys.authToken       -> "RANDOMTOKEN",
      SessionKeys.sensitiveUserId -> "test-user-id"
    )

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override lazy val app: Application = GuiceApplicationBuilder()
    .configure(config)
    .build()

  val mockCredentials: PbikCredentials = PbikCredentials("fake_emp_id")
  val fakeIabd: String                 = "fake_iabd"

  // if reflection stops working, use something like `(100 to 555).sorted`
  private def getAllHttpStatusCodes: Seq[Int] = {
    val statusClass = play.api.http.Status
    statusClass.getClass.getDeclaredFields
      .filter(_.getType == classOf[Int])
      .map { field =>
        field.setAccessible(true)
        field.getInt(statusClass)
      }
  }.toSeq.sorted

  val allPlayFrameworkStatusCodes: Iterable[Int] = getAllHttpStatusCodes

}
