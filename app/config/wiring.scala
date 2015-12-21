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

package config

import com.typesafe.config.Config
import play.api.Play
import uk.gov.hmrc.play.audit.filters.AuditFilter
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.config.{LoadAuditingConfig, AuditingConfig}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.auth.controllers.AuthParamsControllerConfig
import uk.gov.hmrc.play.auth.microservice.connectors.AuthConnector
import uk.gov.hmrc.play.auth.microservice.filters.AuthorisationFilter
import uk.gov.hmrc.play.config.{ServicesConfig, ControllerConfig, RunMode, AppName}
import uk.gov.hmrc.play.http.logging.filters.LoggingFilter
import uk.gov.hmrc.play.http.ws._

object WSHttp extends WSGet with WSPut with WSPost with WSDelete with WSPatch with AppName with RunMode with HttpAuditing {
  override val hooks = Seq(AuditingHook)
  override val auditConnector = PbikAuditConnector
}

object PbikControllerConfig extends ControllerConfig {
  override lazy val controllerConfigs: Config = Play.current.configuration.underlying.getConfig("controllers")
}

object PbikAuthControllerConfig extends AuthParamsControllerConfig {
  override lazy val controllerConfigs: Config = PbikControllerConfig.controllerConfigs
}

object PbikAuditConnector extends AuditConnector with RunMode {
  override lazy val auditingConfig: AuditingConfig = LoadAuditingConfig(s"auditing")
}

object PbikAuthConnector extends AuthConnector with ServicesConfig {
  override def authBaseUrl: String = baseUrl("auth")
}

object PbikLoggingFilter extends LoggingFilter {
  override def controllerNeedsLogging(controllerName: String): Boolean =
    PbikControllerConfig.paramsForController(controllerName).needsLogging
}

object PbikAuditFilter extends AuditFilter with AppName {
  override def auditConnector: AuditConnector = PbikAuditConnector
  override def controllerNeedsAuditing(controllerName: String): Boolean =
    PbikControllerConfig.paramsForController(controllerName).needsAuditing
}

object PbikAuthFilter extends AuthorisationFilter {
  override def authConnector: AuthConnector = PbikAuthConnector
  override def authParamsConfig: AuthParamsControllerConfig = PbikAuthControllerConfig
  override def controllerNeedsAuth(controllerName: String): Boolean =
    PbikControllerConfig.paramsForController(controllerName).needsAuth
}
