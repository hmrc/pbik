ThisBuild / scalaVersion := "2.13.14"
ThisBuild / majorVersion := 8

lazy val microservice = Project("pbik", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(CodeCoverageSettings.settings)
  .settings(
    PlayKeys.playDefaultPort := 9351,
    libraryDependencies ++= AppDependencies(),
    scalacOptions ++= Seq(
      "-feature",
      "-Wconf:src=routes/.*:s"
    )
  )

addCommandAlias("scalafmtAll", "all scalafmtSbt scalafmt Test/scalafmt")
