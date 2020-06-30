import scoverage.ScoverageKeys
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "real-time-income-information"

lazy val scoverageSettings = Seq(
  ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;view.*;config.*;.*(BuildInfo|Routes).*",
  ScoverageKeys.coverageMinimum := 95,
  ScoverageKeys.coverageFailOnMinimum := false,
  ScoverageKeys.coverageHighlighting := true
)
//TODO: default port
lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  .settings(
    libraryDependencies ++= AppDependencies.all,
    retrieveManaged := true,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    publishingSettings,
    scoverageSettings,
    majorVersion := 1,
    resolvers ++= Seq(
      Resolver.jcenterRepo,
      Resolver.bintrayRepo("emueller", "maven"),
      Resolver.bintrayRepo("hmrc", "releases")
    )
  )
