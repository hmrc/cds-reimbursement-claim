import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "cds-reimbursement-claim"

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")
addCommandAlias("fix", "all compile:scalafix test:scalafix")

lazy val wartremoverSettings =
  Seq(
    Compile / compile / wartremoverErrors ++= Warts.allBut(
      Wart.DefaultArguments,
      Wart.ImplicitConversion,
      Wart.ImplicitParameter,
      Wart.Nothing,
      Wart.Overloading,
      Wart.ToString
    ),
    WartRemover.autoImport.wartremoverExcluded += target.value,
    Compile / compile / WartRemover.autoImport.wartremoverExcluded ++=
      (Compile / routes).value ++
        (baseDirectory.value ** "*.sc").get ++
        Seq(sourceManaged.value / "main" / "sbt-buildinfo" / "BuildInfo.scala"),
    Test / compile / wartremoverErrors --= Seq(Wart.NonUnitStatements, Wart.Null, Wart.PublicInference, Wart.Any)
  )

lazy val scoverageSettings =
  Seq(
    ScoverageKeys.coverageExcludedFiles := (Compile / managedSourceDirectories).value
      .map(d => s"${d.getPath}/.*")
      .mkString(";"),
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*(config|views).*",
    ScoverageKeys.coverageMinimumStmtTotal := 80,
    ScoverageKeys.coverageMinimumBranchTotal := 73,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true,
    ThisBuild / Test / test / coverageEnabled := true
  )

lazy val microservice = Project(appName, file("."))
  .enablePlugins(
    play.sbt.PlayScala,
    SbtDistributablesPlugin
  )
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full)
  )
  .settings(scalaVersion := "2.12.14")
  .settings(
    majorVersion := 1,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test
  )
  .settings(
    routesImport := Seq(
      "uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN",
      "uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ReasonForSecurity"
    )
  )
  .settings(TwirlKeys.templateImports := Seq.empty)
  .settings(
    addCompilerPlugin(scalafixSemanticdb),
    scalacOptions ++= List(
      "-Yrangepos",
      "-language:postfixOps",
      "-Ypartial-unification",
      "-Wconf:cat=unused-imports&site=<empty>:iv",
      "-Wconf:cat=unused-imports&site=prod:iv",
      "-Wconf:cat=unused-imports&site=upscan:iv",
      "-Wconf:cat=unused-imports&site=testOnlyDoNotUseInAppConf:iv",
      "-Wconf:cat=unused-privates&site=testOnlyDoNotUseInAppConf.Routes.defaultPrefix:iv"
    ),
    Test / scalacOptions --= Seq("-Ywarn-value-discard")
  )
  .settings(publishingSettings: _*)
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(Test / resourceDirectory := baseDirectory.value / "/conf/resources")
  .settings(wartremoverSettings: _*)
  .settings(scoverageSettings: _*)
  .settings(PlayKeys.playDefaultPort := 7501)
  .settings(scalafmtOnCompile := true)
