import play.sbt.PlayImport.*
import sbt.*

object AppDependencies {

  private lazy val bootstrapPlayVersion = "9.2.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-30" % bootstrapPlayVersion,
    "uk.gov.hmrc" %% "tax-year"                  % "5.0.0"
  )

  val test: Seq[ModuleID]    = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapPlayVersion
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test

}
