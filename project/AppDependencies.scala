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

import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-28" % "5.24.0",
    "uk.gov.hmrc" %% "tax-year"                  % "3.0.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.specs2"             %% "specs2-core"        % "4.16.0",
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0",
    "org.scalatestplus"      %% "mockito-3-4"        % "3.2.10.0",
    "com.vladsch.flexmark"   % "flexmark-all"        % "0.35.10",
    "org.mockito"            % "mockito-all"         % "1.10.19",
    "org.pegdown"            % "pegdown"             % "1.6.0"
  ).map(_ % "test")

  val all: Seq[ModuleID] = compile ++ test

}
