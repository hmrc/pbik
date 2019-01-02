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

package config

import akka.actor.ActorSystem
import com.typesafe.config.Config
import play.api.{Configuration, Play}
import play.api.Mode.Mode
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.HttpHooks
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.auth.controllers.AuthParamsControllerConfig
import uk.gov.hmrc.play.auth.microservice.connectors.AuthConnector
import uk.gov.hmrc.play.auth.microservice.filters.AuthorisationFilter
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, ServicesConfig}
import uk.gov.hmrc.play.http.ws._
import uk.gov.hmrc.play.microservice.config.LoadAuditingConfig
import uk.gov.hmrc.play.microservice.filters.{AuditFilter, LoggingFilter, MicroserviceFilterSupport}


trait RunModeConfig {
  def runModeConfiguration: Configuration = Play.current.configuration
  def mode: Mode = Play.current.mode
}

object MicroserviceAuditConnector extends AuditConnector {
  lazy val auditingConfig: AuditingConfig = LoadAuditingConfig(s"auditing")
}

trait Hooks extends HttpHooks with HttpAuditing {
  override val hooks = Seq(AuditingHook)
  override lazy val auditConnector: AuditConnector = MicroserviceAuditConnector
}

trait WSHttp extends HttpGet with WSGet with HttpPut with WSPut with HttpPost with WSPost with HttpDelete with WSDelete with Hooks with AppName

object WSHttp extends WSHttp {
  override protected def appNameConfiguration: Configuration = Play.current.configuration

  override protected def actorSystem: ActorSystem = Play.current.actorSystem
  override protected val configuration: Option[Config] = Some(Play.current.configuration.underlying)
}


object PbikControllerConfig extends ControllerConfig {
  override lazy val controllerConfigs: Config = Play.current.configuration.underlying.getConfig("controllers")
}

object PbikAuthControllerConfig extends AuthParamsControllerConfig {
  override lazy val controllerConfigs: Config = PbikControllerConfig.controllerConfigs
}

object PbikAuditConnector extends AuditConnector with RunModeConfig {
  override lazy val auditingConfig: AuditingConfig = LoadAuditingConfig(s"auditing")
}

object PbikAuthConnector extends AuthConnector with ServicesConfig with WSHttp with RunModeConfig {
  override def authBaseUrl: String = baseUrl("auth")
  override protected def appNameConfiguration: Configuration = Play.current.configuration

  override protected def actorSystem: ActorSystem = Play.current.actorSystem
  override protected val configuration: Option[Config] = Some(Play.current.configuration.underlying)
}

object PbikLoggingFilter extends LoggingFilter with MicroserviceFilterSupport {
  override def controllerNeedsLogging(controllerName: String): Boolean =
    PbikControllerConfig.paramsForController(controllerName).needsLogging
}

object PbikAuditFilter extends AuditFilter with AppName with MicroserviceFilterSupport with RunModeConfig {
  override def auditConnector: AuditConnector = PbikAuditConnector
  override def controllerNeedsAuditing(controllerName: String): Boolean =
    PbikControllerConfig.paramsForController(controllerName).needsAuditing
  override protected def appNameConfiguration: Configuration = Play.current.configuration
}

object PbikAuthFilter extends AuthorisationFilter with MicroserviceFilterSupport {
  override def authConnector: AuthConnector = PbikAuthConnector
  override def authParamsConfig: AuthParamsControllerConfig = PbikAuthControllerConfig
  override def controllerNeedsAuth(controllerName: String): Boolean =
    PbikControllerConfig.paramsForController(controllerName).needsAuth
}
