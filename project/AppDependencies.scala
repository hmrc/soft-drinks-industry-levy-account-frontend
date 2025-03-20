import sbt._

object AppDependencies {

  private val playSuffix = s"-play-30"
  private val bootstrapVersion = "9.9.0"
  private val hmrcMongoVersion = "2.5.0"
  private val playFrontendHMRCVersion = "11.11.0"

  val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"       %% s"play-frontend-hmrc$playSuffix" % playFrontendHMRCVersion,
    "uk.gov.hmrc"       %% s"bootstrap-frontend$playSuffix" % bootstrapVersion,
    "uk.gov.hmrc"       %% s"play-conditional-form-mapping$playSuffix" % "3.2.0",
    "org.typelevel"     %% "cats-core"                     % "2.12.0",
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo$playSuffix"         % hmrcMongoVersion,
    "uk.gov.hmrc"       %% s"crypto-json$playSuffix"        % "8.2.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.jsoup"               %  "jsoup"                        % "1.18.1",
    "uk.gov.hmrc"             %% s"bootstrap-test$playSuffix"    % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% s"hmrc-mongo-test$playSuffix"   % hmrcMongoVersion,
    "io.github.wolfendale"    %% "scalacheck-gen-regexp"        % "1.1.0"
  ).map(_ % "test, it")

  val all: Seq[ModuleID] = compile ++ test
}
