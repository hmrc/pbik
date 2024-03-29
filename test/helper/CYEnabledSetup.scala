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

import com.google.inject.AbstractModule
import config.PbikConfig
import connectors.HmrcTierConnectorWrapped
import controllers.actions.MinimalAuthAction
import controllers.utils.ControllerUtils
import org.mockito.Mockito.mock

trait CYEnabledSetup {

  object GuiceTestModule extends AbstractModule {

    override def configure(): Unit = {
      bind(classOf[HmrcTierConnectorWrapped]).toInstance(mock(classOf[HmrcTierConnectorWrapped]))
      bind(classOf[ControllerUtils]).toInstance(mock(classOf[ControllerUtils]))
      bind(classOf[PbikConfig]).toInstance(mock(classOf[PbikConfig]))
      bind(classOf[MinimalAuthAction]).to(classOf[TestMinimalAuthAction])
    }
  }

}
