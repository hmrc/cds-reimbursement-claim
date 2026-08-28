import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val playVersion = "-play-30"
  val bootstrapVersion = "10.7.0"
  val hmrcMongoPlayVersion         = "2.13.0"

  val compile = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-backend$playVersion"         % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-work-item-repo$playVersion" % hmrcMongoPlayVersion,
    "org.typelevel"     %% "cats-core"                              % "2.13.0"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-test$playVersion"     % bootstrapVersion,
    "org.playframework"      %% "play-test"                       % current,
    "org.scalatest"          %% "scalatest"                       % "3.2.20",
    "org.scalamock"          %% "scalamock"                       % "6.0.0",
    "org.scalatestplus"      %% "scalacheck-1-18"                 % "3.2.19.0",
    "org.scalatestplus.play" %% "scalatestplus-play"              % "7.0.2",
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-30"         % hmrcMongoPlayVersion,
    "com.vladsch.flexmark"    % "flexmark-all"                    % "0.64.8",
    "org.apache.pekko"       %% "pekko-testkit"                   % "1.0.3",
    "org.scala-lang.modules" %% "scala-xml"                       % "2.4.0",
    "uk.gov.hmrc"            %% "play-json-schema-validator"      % "0.1.0"
  ).map(_ % Test)
}
