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
import com.google.inject.{ImplementedBy, Inject}
import com.typesafe.config.Config
import play.api.Mode.Mode
import play.api.{Configuration, Play}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.{HttpHook, HttpHooks}
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

//TODO remove
trait RunModeConfig {
  def runModeConfiguration: Configuration = Play.current.configuration
  def mode: Mode = Play.current.mode
}

object MicroserviceAuditConnector extends AuditConnector {
  lazy val auditingConfig: AuditingConfig = LoadAuditingConfig(s"auditing")
}

trait Hooks extends HttpHooks with HttpAuditing {
  override val hooks: Seq[HttpHook] = Seq(AuditingHook)
  override lazy val auditConnector: AuditConnector = MicroserviceAuditConnector
}

@ImplementedBy(classOf[WSHttpImpl])
trait WSHttp extends HttpGet with WSGet with HttpPut with WSPut with HttpPost with WSPost with HttpDelete with WSDelete with Hooks with AppName

class WSHttpImpl @Inject()(
                            config: Configuration,
                            val actorSystem: ActorSystem
                          ) extends WSHttp {
  override val appNameConfiguration: Configuration = config
  override protected val configuration: Option[Config] = Some(config.underlying)
}

class PbikControllerConfig @Inject()(configuration: Configuration) extends ControllerConfig {
  override lazy val controllerConfigs: Config = configuration.underlying.getConfig("controllers")
}

class PbikAuthControllerConfig @Inject()(controllerConfig: PbikControllerConfig) extends AuthParamsControllerConfig {
  override lazy val controllerConfigs: Config = controllerConfig.controllerConfigs
}

object PbikAuditConnector extends AuditConnector with RunModeConfig {
  override lazy val auditingConfig: AuditingConfig = LoadAuditingConfig(s"auditing")
}

class PbikAuthConnector @Inject()(
                                   val appNameConfiguration: Configuration,
                                   val actorSystem: ActorSystem
                                 ) extends AuthConnector with ServicesConfig with WSHttp with RunModeConfig {

  override def authBaseUrl: String = baseUrl("auth")

  override protected val configuration: Option[Config] = Some(appNameConfiguration.underlying)
}

class PbikLoggingFilter @Inject()(pbikControllerConfig: PbikControllerConfig) extends LoggingFilter with MicroserviceFilterSupport {
  override def controllerNeedsLogging(controllerName: String): Boolean =
    pbikControllerConfig.paramsForController(controllerName).needsLogging
}

class PbikAuditFilter @Inject()(
                                 pbikControllerConfig: PbikControllerConfig,
                                 configuration: Configuration
                               ) extends AuditFilter with AppName with MicroserviceFilterSupport with RunModeConfig {
  //TODO Inject
  override def auditConnector: AuditConnector = PbikAuditConnector

  override def controllerNeedsAuditing(controllerName: String): Boolean =
    pbikControllerConfig.paramsForController(controllerName).needsAuditing

  override protected def appNameConfiguration: Configuration = configuration
}

class PbikAuthFilter @Inject()(
                                val authConnector: PbikAuthConnector,
                                val authParamsConfig: PbikAuthControllerConfig,
                                pbikControllerConfig: PbikControllerConfig
                              ) extends AuthorisationFilter with MicroserviceFilterSupport {
  override def controllerNeedsAuth(controllerName: String): Boolean =
    pbikControllerConfig.paramsForController(controllerName).needsAuth
}
