import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val bootstrapVersion = "8.4.0"

  val compile = Seq(
    "uk.gov.hmrc"           %% "bootstrap-backend-play-30"         % bootstrapVersion,
    "uk.gov.hmrc.mongo"     %% "hmrc-mongo-work-item-repo-play-30" % "1.7.0",
    "org.typelevel"         %% "cats-core"                         % "2.10.0",
    "com.github.kxbmap"     %% "configs"                           % "0.6.1",
    "ru.tinkoff"            %% "phobos-core"                       % "0.21.0",
    "com.github.arturopala" %% "play-json-extensions"              % "1.0.0"
  )

  val test = Seq(
    "uk.gov.hmrc"                %% "bootstrap-test-play-30"     % bootstrapVersion % Test,
    "org.playframework"          %% "play-test"                  % current          % Test,
    "org.scalatest"              %% "scalatest"                  % "3.2.18"         % Test,
    "org.scalamock"              %% "scalamock"                  % "5.2.0"          % Test,
    "org.scalatestplus"          %% "scalacheck-1-17"            % "3.2.18.0"       % Test,
    "com.github.alexarchambault" %% "scalacheck-shapeless_1.16"  % "1.3.1"          % Test,
    "org.scalatestplus.play"     %% "scalatestplus-play"         % "7.0.1"          % Test,
    "uk.gov.hmrc.mongo"          %% "hmrc-mongo-test-play-30"    % "1.7.0"          % Test,
    "com.github.chocpanda"       %% "scalacheck-magnolia"        % "0.6.0"          % Test,
    "com.vladsch.flexmark"        % "flexmark-all"               % "0.64.0"         % Test,
    "org.pegdown"                 % "pegdown"                    % "1.6.0"          % Test,
    "org.apache.pekko"           %% "pekko-testkit"              % "1.0.2"          % Test,
    "org.scala-lang.modules"     %% "scala-xml"                  % "2.2.0"          % Test,
    "com.eclipsesource"          %% "play-json-schema-validator" % "0.9.5"          % Test
  )
}
