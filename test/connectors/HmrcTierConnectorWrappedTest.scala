/*
 * Copyright 2015 HM Revenue & Customs
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

import controllers.{GatewayNPSController, FakePBIKApplication}
import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.scalatest.mock.MockitoSugar
import org.specs2.mock.mockito.MockitoMatchers
import play.api.libs.json.Json
import play.api.test.FakeApplication
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{RunMode, AppName, ServicesConfig}
import uk.gov.hmrc.play.http.HttpResponse
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.play.http.ws._

class HmrcTierConnectorWrappedTest extends UnitSpec with MockitoSugar with MockitoMatchers with FakePBIKApplication {

  class FakeResponse extends HttpResponse {
    override val allHeaders = Map[scala.Predef.String, scala.Seq[scala.Predef.String]]()
    override def status = 200
    override val json = Json.parse(sampleBikJson)
    override val body = sampleBikJson

  }

  object StubbedWSHttp extends scala.AnyRef with WSGet with WSPut with WSPost with WSDelete with WSPatch with AppName with RunMode with HttpAuditing {

    override val hooks = Seq(AuditingHook)
    override def auditConnector = mock[AuditConnector]

    override def doGet(url : scala.Predef.String)(implicit hc : uk.gov.hmrc.play.http.HeaderCarrier) :
    scala.concurrent.Future[uk.gov.hmrc.play.http.HttpResponse] = new FakeResponse

    override def doPost[A](url : scala.Predef.String, body : A, headers :
    scala.Seq[scala.Tuple2[scala.Predef.String, scala.Predef.String]])
                          (implicit rds : play.api.libs.json.Writes[A], hc : uk.gov.hmrc.play.http.HeaderCarrier) :
    scala.concurrent.Future[uk.gov.hmrc.play.http.HttpResponse] = new FakeResponse


  }

  class StubbedTier extends HmrcTierConnectorWrapped with TierClient with ServicesConfig {
    override val http = StubbedWSHttp
    override val serviceOriginatorIdKey = getConfString("nps.originatoridkey", "")
    override val serviceOriginatorId = getConfString("nps.originatoridvalue", "")
  }

//  "When getting data the Controller " should {
//    " parse a response correctly and not mutate the returned response body " in {
//      running( new FakeApplication() ) {
//        val stubbo = new StubbedTier
//        val CY = 2015
//        val result = await(stubbo.retrieveDataGet("https://"))
//        assert(result.status == 200)
//      }
//    }
//  }
//
//  "When posting data the Controller " should {
//    " parse a response correctly and not mutate the returned response body " in {
//      running( new FakeApplication() ) {
//        val stubbo = new StubbedTier
//        val CY = 2015
//        val result = await(stubbo.retrieveDataPost(GatewayNPSController.NO_HEADERS,"https://",Json.parse(sampleBikJson))(hc))
//        assert(result.status == 200)
//      }
//    }
//  }

}
