import play.sbt.routes.RoutesKeys
import scoverage.ScoverageKeys
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "real-time-income-information"

lazy val scoverageSettings = {
  val sCoverageExcludesPattens = List(
    "<empty>",
    "Reverse.*",
    "view.*",
    "config.*",
    ".*(BuildInfo|Routes).*",
    "com.kenshoo.play.*",
    "controllers.javascript",
    ".*Reverse.*Controller"
  )
  Seq(
    ScoverageKeys.coverageExcludedPackages := sCoverageExcludesPattens.mkString(";"),
    ScoverageKeys.coverageMinimum := 95,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    scalaVersion := "2.12.10",
    libraryDependencies ++= AppDependencies.all,
    retrieveManaged := true,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    publishingSettings,
    PlayKeys.playDefaultPort := 9358,
    scoverageSettings,
    RoutesKeys.routesImport := Nil,
    TwirlKeys.templateImports := Nil,
    scalacOptions += "-Xfatal-warnings",
    majorVersion := 2,
    resolvers ++= Seq(
      Resolver.jcenterRepo,
      Resolver.bintrayRepo("emueller", "maven"),
      Resolver.bintrayRepo("hmrc", "releases")
    )
  )
