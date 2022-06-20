import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport.scalafmtOnCompile

val appName = "pbik"

lazy val scoverageSettings: Seq[Def.Setting[_]] = {
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;models/.data/..*;views.*;config.*;models.*;" +
      ".*(AuthService|BuildInfo|Routes).*;" +
      "connectors.*",
    ScoverageKeys.coverageMinimumStmtTotal := 80.22,
    ScoverageKeys.coverageMinimumBranchTotal := 62.50,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val plugins: Seq[Plugins] =
  Seq(play.sbt.PlayScala, SbtDistributablesPlugin)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(plugins: _*)
  .settings(
    scoverageSettings,
    scalaSettings,
    scalaVersion := "2.12.15",
    publishingSettings,
    defaultSettings(),
    majorVersion := 4,
    scalafmtOnCompile := true,
    PlayKeys.playDefaultPort := 9351,
    libraryDependencies ++= AppDependencies.all,
    retrieveManaged := true,
    routesGenerator := InjectedRoutesGenerator,
    resolvers += Resolver.jcenterRepo
  )
