
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}
import uk.gov.hmrc.{SbtAutoBuildPlugin, _}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "pbik"

lazy val scoverageSettings: Seq[Def.Setting[_]] = {
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;models/.data/..*;views.*;config.*;models.*;" +
      ".*(AuthService|BuildInfo|Routes).*;" +
      "connectors.*",
    ScoverageKeys.coverageMinimum := 80,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val plugins: Seq[Plugins] = Seq(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(plugins: _*)
  .settings(
    scoverageSettings,
    scalaSettings,
    publishingSettings,
    defaultSettings(),
    majorVersion := 4,
    PlayKeys.playDefaultPort := 9351,
    libraryDependencies ++= AppDependencies.all,
    retrieveManaged := true,
    routesGenerator := StaticRoutesGenerator,
    resolvers ++= Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      Resolver.jcenterRepo
    )
  )
