import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val bootstrapVersion = "7.3.0"

  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-28"         % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-work-item-repo-play-28" % "0.71.0",
    "org.typelevel"     %% "cats-core"                         % "2.9.0",
    "org.julienrf"      %% "play-json-derived-codecs"          % "10.1.0",
    "com.github.kxbmap" %% "configs"                           % "0.6.1",
    "ru.tinkoff"        %% "phobos-core"                       % "0.20.0",
    "ai.x"              %% "play-json-extensions"              % "0.42.0"
  )

  val test = Seq(
    "uk.gov.hmrc"                %% "bootstrap-test-play-28"     % bootstrapVersion   % Test,
    "com.typesafe.play"          %% "play-test"                  % current   % Test,
    "org.scalatest"              %% "scalatest"                  % "3.2.15"   % Test,
    "org.scalamock"              %% "scalamock"                  % "5.2.0"   % Test,
    "org.scalatestplus"          %% "scalacheck-1-14"            % "3.2.2.0" % Test,
    "com.github.alexarchambault" %% "scalacheck-shapeless_1.16"  % "1.3.1"   % Test,
    "org.scalatestplus.play"     %% "scalatestplus-play"         % "5.1.0"   % Test,
    "uk.gov.hmrc.mongo"          %% "hmrc-mongo-test-play-28"    % "0.71.0"  % Test,
    "com.github.chocpanda"       %% "scalacheck-magnolia"        % "0.6.0"   % Test,
    "com.vladsch.flexmark"        % "flexmark-all"               % "0.64.0"  % Test,
    "org.pegdown"                 % "pegdown"                    % "1.6.0"   % Test,
    "com.typesafe.akka"          %% "akka-testkit"               % "2.8.0"  % Test,
    "org.scala-lang.modules"     %% "scala-xml"                  % "2.1.0"   % Test,
    "com.eclipsesource"          %% "play-json-schema-validator" % "0.9.5"   % Test
  )
}
