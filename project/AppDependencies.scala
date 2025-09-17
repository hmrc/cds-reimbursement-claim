import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val bootstrapVersion = "10.1.0"

  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30"         % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-work-item-repo-play-30" % "2.7.0",
    "org.typelevel"     %% "cats-core"                         % "2.13.0"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"     % bootstrapVersion % Test,
    "org.playframework"      %% "play-test"                  % current          % Test,
    "org.scalatest"          %% "scalatest"                  % "3.2.19"         % Test,
    "org.scalamock"          %% "scalamock"                  % "6.0.0"          % Test,
    "org.scalatestplus"      %% "scalacheck-1-18"            % "3.2.19.0"       % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"         % "7.0.2"          % Test,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-30"    % "2.7.0"          % Test,
    "com.vladsch.flexmark"    % "flexmark-all"               % "0.64.8"         % Test,
    "org.apache.pekko"       %% "pekko-testkit"              % "1.0.3"          % Test,
    "org.scala-lang.modules" %% "scala-xml"                  % "2.3.0"          % Test,
    "uk.gov.hmrc"            %% "play-json-schema-validator" % "0.1.0"          % Test
  )
}
