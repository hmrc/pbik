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

import sbt._

object MicroServiceBuild extends Build with MicroService {
  import scala.util.Properties.envOrElse

  val appName = "pbik"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  import play.PlayImport._
  import play.core.PlayVersion

  private val playMicroServiceBootstrapVersion = "4.2.1"
  private val playHealthVersion = "1.1.0"
  private val playHttpVerbVersion = "3.3.0"
  private val playConfigVersion = "2.0.1"
  private val playAuthorisation = "3.1.0"
  private val playJsonEncoder = "2.1.0"
  private val metricsGraphiteVersion = "3.0.2"
  private val hmrcTestVersion = "1.4.0"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "microservice-bootstrap" % playMicroServiceBootstrapVersion,
    "uk.gov.hmrc" %% "play-config" % playConfigVersion,
    "uk.gov.hmrc" %% "play-health" % playHealthVersion,
    "uk.gov.hmrc" %% "http-verbs" % playHttpVerbVersion,
    "com.codahale.metrics" % "metrics-graphite" % metricsGraphiteVersion,
    "uk.gov.hmrc" %% "play-authorisation" % playAuthorisation,
    "uk.gov.hmrc" %% "play-json-logger" % playJsonEncoder
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.scalatest" %% "scalatest" % "2.2.0" % scope,
        "org.pegdown" % "pegdown" % "1.4.2" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope
      )
    }.test
  }

  def apply() = compile ++ Test()
}

