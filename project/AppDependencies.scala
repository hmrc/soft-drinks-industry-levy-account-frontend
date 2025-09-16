import sbt._

object AppDependencies {

  private val playVersion = s"-play-30"
  private val bootstrapVersion = "9.9.0"
  private val hmrcMongoVersion = "2.5.0"
  private val playFrontendHMRCVersion = "11.11.0"

  val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"       %% s"play-frontend-hmrc$playVersion" % playFrontendHMRCVersion,
    "uk.gov.hmrc"       %% s"bootstrap-frontend$playVersion" % bootstrapVersion,
    "uk.gov.hmrc"       %% s"play-conditional-form-mapping$playVersion" % "3.2.0",
    "org.typelevel"     %% "cats-core"                     % "2.12.0",
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo$playVersion"         % hmrcMongoVersion,
    "uk.gov.hmrc"       %% s"crypto-json$playVersion"        % "8.2.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.jsoup"               %  "jsoup"                        % "1.18.1",
    "uk.gov.hmrc"             %% s"bootstrap-test$playVersion"    % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% s"hmrc-mongo-test$playVersion"   % hmrcMongoVersion,
    "io.github.wolfendale"    %% "scalacheck-gen-regexp"        % "1.1.0"
  ).map(_ % "test, it")

  val all: Seq[ModuleID] = compile ++ test
}
