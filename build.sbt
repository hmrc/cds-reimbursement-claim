import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings

val appName = "cds-reimbursement-claim"

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

lazy val scoverageSettings =
  Seq(
    ScoverageKeys.coverageExcludedFiles := (Compile / managedSourceDirectories).value
      .map(d => s"${d.getPath}/.*")
      .mkString(";"),
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*(config|views).*",
    ScoverageKeys.coverageMinimumStmtTotal := 80,
    ScoverageKeys.coverageMinimumBranchTotal := 73,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )

lazy val microservice = Project(appName, file("."))
  .enablePlugins(
    play.sbt.PlayScala,
    SbtAutoBuildPlugin,
    SbtGitVersioning,
    SbtDistributablesPlugin
  )
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.3" cross CrossVersion.full)
  )
  .settings(scalaVersion := "2.13.15")
  .settings(
    majorVersion := 1,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test
  )
  .settings(
    routesImport := Seq(
      "uk.gov.hmrc.cdsreimbursementclaim.models.ids.Eori",
      "uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN",
      "uk.gov.hmrc.cdsreimbursementclaim.models.tpi01.ClaimsSelector",
      "uk.gov.hmrc.cdsreimbursementclaim.models.CDFPayService",
      "uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ReasonForSecurity"
    )
  )
  .settings(TwirlKeys.templateImports := Seq.empty)
  .settings(
    scalacOptions ++= List(
      "-Xmigration",
      "-Yrangepos",
      "-Xlint:-byname-implicit",
      "-language:postfixOps",
      "-Wconf:cat=unused-imports&site=<empty>:iv",
      "-Wconf:cat=unused-imports&site=prod:iv",
      "-Wconf:cat=unused-imports&site=upscan:iv",
      "-Wconf:cat=unused-imports&site=testOnlyDoNotUseInAppConf:iv",
      "-Wconf:cat=unused-privates&site=testOnlyDoNotUseInAppConf.Routes.defaultPrefix:iv",
      s"-Wconf:src=${target.value}/scala-${scalaBinaryVersion.value}/routes/.*:s,src=${target.value}/scala-${scalaBinaryVersion.value}/twirl/.*:s"
    ),
    Test / scalacOptions --= Seq("-Ywarn-value-discard")
  )
  .settings(Test / resourceDirectory := baseDirectory.value / "/conf/resources")
  .settings(scoverageSettings: _*)
  .settings(PlayKeys.playDefaultPort := 7501)
  .settings(scalafmtOnCompile := true)
