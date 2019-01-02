/*
 * Copyright 2019 HM Revenue & Customs
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

import controllers.FakePBIKApplication
import org.scalatest.mock.MockitoSugar
import org.specs2.mock.mockito.MockitoMatchers
import play.api.test.FakeApplication
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.test.UnitSpec
import play.api.test.Helpers._

class URIInformationTest extends UnitSpec with MockitoSugar with MockitoMatchers with FakePBIKApplication {

  trait StubServicesConfig extends ServicesConfig {
    override def baseUrl(serviceName:String) = "http://nps:80"
  }

  "When loading config the URIInformation " should {
    " load the correct getBenefitTypesPath " in {
      running( new FakeApplication() ) {
        new {
          val testSplunker = "testSplunker"
        } with URIInformation {

          assert(getBenefitTypesPath == "getbenefittypes")
        }
      }
    }
  }

  "When loading config the URIInformation " should {
    " load the correct updateBenefitTypesPath " in {
      running( new FakeApplication() ) {
        new {
          val testSplunker = "testSplunker"
        } with URIInformation {
          assert(updateBenefitTypesPath == "update")
        }
      }
    }
  }

  "When loading config the URIInformation " should {
    " load the correct exclusionPath " in {
      running( new FakeApplication() ) {
        new {
          val testSplunker = "testSplunker"
        } with URIInformation {
         assert(exclusionPath == "exclusion")
        }
      }
    }
  }

  "When loading config the URIInformation " should {
    " load the correct addExclusionPath " in {
      running( new FakeApplication() ) {
        new {
          val testSplunker = "testSplunker"
        } with URIInformation {
          assert(addExclusionPath == "exclusion/update")
        }
      }
    }
  }

  "When loading config the URIInformation " should {
    " load the correct removeExclusionPath " in {
      running( new FakeApplication() ) {
        new {
          val testSplunker = "testSplunker"
        } with URIInformation {
          assert(removeExclusionPath == "exclusion/remove")
        }
      }
    }
  }

}
