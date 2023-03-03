/*
 * Copyright 2023 HM Revenue & Customs
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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.mockito.MockitoSugar
import play.api.Configuration
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, RequestId}

import scala.concurrent.ExecutionContext.Implicits.global

class HmrcTierConnectorWrappedSpec extends AnyWordSpec with MockitoSugar with Matchers {

  trait Setup {

    val mockHttpClient: HttpClient          = mock[HttpClient]
    val mockAppConfig: Configuration        = mock[Configuration]
    val uuid                                = "123f4567-g89c-42c3-b456-557742330000"
    val connector: HmrcTierConnectorWrapped = new HmrcTierConnectorWrapped(mockHttpClient, mockAppConfig) {
      override def generateNewUUID: String = uuid
    }
  }

  "requestID is present in the headerCarrier" should {
    "return new ID pre-appending the requestID when the requestID matches the format(8-4-4-4)" in new Setup {
      val requestId = "dcba0000-ij12-df34-jk56"
      connector.getCorrelationId(HeaderCarrier(requestId = Some(RequestId(requestId)))) shouldBe
        s"$requestId-${uuid.substring(24)}"
    }

    "return new ID when the requestID does not match the format(8-4-4-4)" in new Setup {
      val requestId = "1a2b-ij12-df34-jk56"
      connector.getCorrelationId(HeaderCarrier(requestId = Some(RequestId(requestId)))) shouldBe uuid
    }
  }

  "requestID is not present in the headerCarrier should return a new ID" should {
    "return the uuid" in new Setup {
      connector.getCorrelationId(HeaderCarrier()) shouldBe uuid
    }
  }

}
