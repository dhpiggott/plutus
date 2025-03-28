import scala.scalanative.build._

addCommandAlias("fmtCheck", "all scalafmtSbtCheck scalafmtCheckAll")
addCommandAlias("fmt", "all scalafmtSbt scalafmtAll")
addCommandAlias("fixCheck", "scalafixAll --check")
addCommandAlias("fix", "scalafixAll")
addCommandAlias("check", "fmtCheck; fixCheck; dependencyUpdates")

enablePlugins(BuildInfoPlugin, ScalaNativePlugin, Smithy4sCodegenPlugin)

Global / onChangedBuildSource := ReloadOnSourceChanges

name := "plutus"

scalaVersion := "3.6.2"

semanticdbEnabled := true

libraryDependencies ++= Seq(
  // TODO: Experiment with a backend that uses
  // https://github.com/armanbilge/porcupine to write directly to GnuCash's
  // database.
  "co.fs2" %%% "fs2-core" % "3.12.0",
  "co.fs2" %%% "fs2-io" % "3.12.0",
  "com.disneystreaming.smithy4s" %%% "smithy4s-cats" % smithy4sVersion.value,
  "com.disneystreaming.smithy4s" %%% "smithy4s-core" % smithy4sVersion.value,
  "com.disneystreaming.smithy4s" %%% "smithy4s-http4s" % smithy4sVersion.value,
  "com.disneystreaming.smithy4s" %%% "smithy4s-http4s-kernel" % smithy4sVersion.value,
  "com.disneystreaming.smithy4s" %%% "smithy4s-json" % smithy4sVersion.value,
  "com.disneystreaming.smithy4s" %%% "smithy4s-xml" % smithy4sVersion.value,
  "com.lihaoyi" %%% "fansi" % "0.4.0",
  "com.monovore" %%% "decline" % "2.4.1",
  "com.monovore" %%% "decline-effect" % "2.4.1",
  "org.gnieh" %%% "fs2-data-text" % "1.11.2",
  "org.gnieh" %%% "fs2-data-xml" % "1.11.2",
  "org.http4s" %%% "http4s-client" % "0.23.30",
  "org.http4s" %%% "http4s-core" % "0.23.30",
  "org.http4s" %%% "http4s-dsl" % "0.23.30",
  "org.http4s" %%% "http4s-ember-client" % "0.23.30",
  "org.http4s" %%% "http4s-ember-server" % "0.23.30",
  "org.http4s" %%% "http4s-server" % "0.23.30",
  "org.typelevel" %% "cats-core" % "2.13.0",
  "org.typelevel" %%% "cats-effect" % "3.6.0",
  "org.typelevel" %%% "cats-effect-kernel" % "3.6.0",
  "org.typelevel" %%% "cats-effect-std" % "3.6.0",
  "org.typelevel" %%% "case-insensitive" % "1.4.2"
)

dependencyUpdatesFailBuild := true

buildInfoKeys := Seq(version)

buildInfoPackage := "plutus"

crossPaths := false

nativeConfig ~= (_.withMode(Mode.releaseSize))
