import play.sbt.routes.RoutesKeys
import sbt.Def
import scoverage.ScoverageKeys
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion
import scala.collection.Seq

lazy val appName: String = "soft-drinks-industry-levy-account-frontend"

lazy val scoverageSettings = {
  Seq(
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*handlers.*;.*components.*;" +
      ".*Routes.*;.*viewmodels.*;.*views.*;.*CascadeUpsert*.*GuiceInjector;.*\\$anon.*;.*javascript;testOnlyDoNotUseInAppConf.*",
    ScoverageKeys.coverageMinimumStmtTotal := 91,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    scalacOptions ++= Seq(
      "-feature",
      "-Wconf:cat=deprecation:s"
    ),
    scalacOptions := scalacOptions.value.distinct,
    ScoverageKeys.coverageExcludedPackages := ".*\\$anon.*"
  )
}

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(inConfig(Test)(testSettings): _*)
  .settings(majorVersion := 0, libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always)
  .settings(ThisBuild / useSuperShell := false)
  .settings(scoverageSettings: _*)
  .settings(
    scalaVersion := "3.7.1",
    name := appName,
    RoutesKeys.routesImport ++= Seq(
      "models._",
      "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl"
    ),
    TwirlKeys.templateImports ++= Seq(
      "play.twirl.api.HtmlFormat",
      "play.twirl.api.HtmlFormat._",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
      "uk.gov.hmrc.hmrcfrontend.views.config._",
      "views.ViewUtils._",
      "controllers.routes._",
      "viewmodels.govuk.all._"
    ),
    PlayKeys.playDefaultPort := 8707,
    libraryDependencies ++= AppDependencies.all,
    retrieveManaged := true,
    Concat.groups := Seq(
      "javascripts/application.js" -> group(Seq(
        "javascripts/app.js"
      ))
    ),
    scalacOptions ++= Seq(
      "-feature",
      "-Wconf:cat=feature:s,src=target/.*:s,msg=Flag.*repeatedly:s,msg=unused explicit parameter*:s",
      "-deprecation",
      "-unchecked",
      "-encoding", "UTF-8",
      "-feature",
      "-Wconf:src=routes/.*:s,src=views/.*txt.*:s,msg=unused import*:s"
    ),
    scalacOptions := scalacOptions.value.distinct,
    uglifyCompressOptions := Seq("unused=false", "dead_code=false"),
    pipelineStages := Seq(digest),
    Assets / pipelineStages := Seq(concat, uglify),
    uglify / includeFilter := GlobFilter("application.js")
  )

lazy val it = (project in file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(root % "compile->compile;test->test")
  .settings(
    name := s"$appName-integration-tests",
    scalaVersion := "3.7.1",
    majorVersion := 0,
    libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
    libraryDependencies ++= AppDependencies.all,
    Test / fork := true,
    Test / parallelExecution := false,
    Test / unmanagedSourceDirectories := Seq(
      baseDirectory.value / "connectors",
      baseDirectory.value / "controllers",
      baseDirectory.value / "repositories",
      baseDirectory.value / "resources",
      baseDirectory.value / "testSupport",
      (root / baseDirectory).value / "test-utils"
    ),
    Test / unmanagedResourceDirectories := Seq(
      baseDirectory.value / "resources"
    )
  )

lazy val testSettings: Seq[Def.Setting[_]] = Seq(
  fork := true,
  unmanagedSourceDirectories += baseDirectory.value / "test-utils"
)