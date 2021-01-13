import play.core.PlayVersion.current
import sbt._

object AppDependencies {
  val silencerVersion = "1.7.0"

  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-26" % "3.0.0",
    "uk.gov.hmrc"       %% "simple-reactivemongo"      % "7.30.0-play-26",
    "uk.gov.hmrc"       %% "work-item-repo"            % "7.10.0-play-26",
    "org.typelevel"     %% "cats-core"                 % "2.3.1",
    "org.julienrf"      %% "play-json-derived-codecs"  % "7.0.0",
    "com.github.kxbmap" %% "configs"                   % "0.5.0",
    compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
    "com.github.ghik"    % "silencer-lib"              % silencerVersion % Provided cross CrossVersion.full
  )

  val test = Seq(
    "org.scalatest"              %% "scalatest"                  % "3.0.8"          % "test",
    "com.typesafe.play"          %% "play-test"                  % current          % "test",
    "org.scalamock"              %% "scalamock"                  % "4.2.0"          % "test",
    "com.github.alexarchambault" %% "scalacheck-shapeless_1.14"  % "1.2.1"          % "test",
    "org.pegdown"                 % "pegdown"                    % "1.6.0"          % "test, it",
    "org.scalatestplus.play"     %% "scalatestplus-play"         % "3.1.2"          % "test, it",
    "uk.gov.hmrc"                %% "reactivemongo-test"         % "4.21.0-play-26" % "test"
  )
}
