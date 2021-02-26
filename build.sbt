import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "cds-reimbursement-claim"

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")
addCommandAlias("fix", "all compile:scalafix test:scalafix")

lazy val wartremoverSettings =
  Seq(
    wartremoverErrors in (Compile, compile) ++= Warts.allBut(
      Wart.DefaultArguments,
      Wart.ImplicitConversion,
      Wart.ImplicitParameter,
      Wart.Nothing,
      Wart.Overloading,
      Wart.ToString
    ),
    WartRemover.autoImport.wartremoverExcluded += target.value,
    WartRemover.autoImport.wartremoverExcluded in (Compile, compile) ++=
      routes.in(Compile).value ++
        (baseDirectory.value ** "*.sc").get ++
        Seq(sourceManaged.value / "main" / "sbt-buildinfo" / "BuildInfo.scala"),
    wartremoverErrors in (Test, compile) --= Seq(Wart.NonUnitStatements, Wart.Null, Wart.PublicInference, Wart.Any)
  )

lazy val scoverageSettings =
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*Reverse.*;.*(config|views.*);.*(BuildInfo|Routes).*",
    ScoverageKeys.coverageMinimum := 80.00,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true,
    coverageEnabled.in(ThisBuild, Test, test) := true
  )

lazy val microservice = Project(appName, file("."))
  .enablePlugins(
    play.sbt.PlayScala,
    SbtAutoBuildPlugin,
    SbtGitVersioning,
    SbtDistributablesPlugin,
    SbtArtifactory
  )
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)
  )
  .settings(scalaVersion := "2.12.12")
  .settings(
    majorVersion := 0,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test
  )
  .settings(routesImport := Seq.empty)
  .settings(TwirlKeys.templateImports := Seq.empty)
  .settings(
    addCompilerPlugin(scalafixSemanticdb),
    scalacOptions ++= List(
      "-Yrangepos",
      "-language:postfixOps",
      "-Ypartial-unification"
    ),
    scalacOptions in Test --= Seq("-Ywarn-value-discard"),
    scalacOptions += "-P:silencer:pathFilters=routes"
  )
  .settings(publishingSettings: _*)
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(Test / resourceDirectory := baseDirectory.value / "/conf/resources")
  .settings(wartremoverSettings: _*)
  .settings(scoverageSettings: _*)
  .settings(PlayKeys.playDefaultPort := 7501)
  .settings(scalafmtOnCompile := true)