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
  import play.sbt.PlayImport._
  import play.core.PlayVersion

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "microservice-bootstrap" % "10.4.0",
    "uk.gov.hmrc" %% "tax-year" % "0.5.0"

  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = Seq.empty
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % "3.6.0-play-25" % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1" % scope,
        "org.specs2" % "specs2_2.10" % "2.3.13",
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "uk.gov.hmrc" %% "tax-year" % "0.5.0" % scope
      )
    }.test
  }

  def apply() = compile ++ Test()
}

