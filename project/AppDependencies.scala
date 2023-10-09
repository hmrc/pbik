import play.sbt.PlayImport.*
import sbt.*

object AppDependencies {

  private lazy val bootstrapPlayVersion = "7.22.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-28" % bootstrapPlayVersion,
    "uk.gov.hmrc" %% "tax-year"                  % "3.3.0"
  )

  val test: Seq[ModuleID]    = Seq(
    "org.scalatest"       %% "scalatest"              % "3.2.17",
    "uk.gov.hmrc"         %% "bootstrap-test-play-28" % bootstrapPlayVersion,
    "org.mockito"         %% "mockito-scala"          % "1.17.27",
    "com.vladsch.flexmark" % "flexmark-all"           % "0.64.8"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test

}
