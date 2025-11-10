import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val bootstrapVersion = "9.19.0"

  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30"         % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-work-item-repo-play-30" % "2.10.0",
    "org.typelevel"     %% "cats-core"                         % "2.13.0"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"     % bootstrapVersion,
    "org.playframework"      %% "play-test"                  % current,
    "org.scalatest"          %% "scalatest"                  % "3.2.19",
    "org.scalamock"          %% "scalamock"                  % "6.0.0",
    "org.scalatestplus"      %% "scalacheck-1-18"            % "3.2.19.0",
    "org.scalatestplus.play" %% "scalatestplus-play"         % "7.0.1",
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-30"    % "2.6.0",
    "com.vladsch.flexmark"    % "flexmark-all"               % "0.64.8",
    "org.apache.pekko"       %% "pekko-testkit"              % "1.0.3",
    "org.scala-lang.modules" %% "scala-xml"                  % "2.3.0",
    "io.github.arturopala"   %% "play-json-schema-validator" % "1.1.0"
  ).map(_ % Test)
}
