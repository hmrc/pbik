/*
 * Copyright 2016 HM Revenue & Customs
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

import play.api.libs.ws.WS
import play.api.libs.ws.WS.WSRequestHolder
import uk.gov.hmrc.play.test.UnitSpec
import org.scalatest.mock._
import play.api.Play.current
import play.api.test._
import play.api.test.Helpers._
import play.api.Logger
import models.HeaderTags

class ConnectorTests extends UnitSpec with MockitoSugar {

  "The HmrcTierConnector " should {
    "always be defined with a concrete tier connector implementaion" in {

      val wrapped = new HmrcTierConnectorWrapped {}
      assert(wrapped != null)

    }
  }

}
