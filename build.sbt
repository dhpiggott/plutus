import org.typelevel.scalacoptions.ScalacOptions

Global / onChangedBuildSource := ReloadOnSourceChanges

name := "plutus"

inThisBuild(
  Seq(
    scalaVersion := "3.7.1",
    semanticdbEnabled := true
  )
)

lazy val macos = project
  .enablePlugins(
    BindgenPlugin
  )
  .settings(
    dependencyUpdatesFailBuild := true,
    bindgenVersion := "0.2.4",
    bindgenBindings += bindgen.interface
      .Binding((Compile / baseDirectory).value / "macos.h", "macos")
      .addCImport(
        "CoreFoundation/CFString.h"
      )
      .withLogLevel(bindgen.interface.LogLevel.Info),
    tpolecatExcludeOptions ++= Set(
      ScalacOptions.deprecation,
      ScalacOptions.warnUnusedImports
    )
  )

lazy val plutus = project
  .dependsOn(macos)
  .enablePlugins(
    BuildInfoPlugin,
    ScalaNativePlugin,
    Smithy4sCodegenPlugin
  )
  .settings(
    dependencyUpdatesFailBuild := true,
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
      "org.gnieh" %%% "fs2-data-text" % "1.12.0",
      "org.gnieh" %%% "fs2-data-xml" % "1.12.0",
      "org.http4s" %%% "http4s-client" % "0.23.30",
      "org.http4s" %%% "http4s-core" % "0.23.30",
      "org.http4s" %%% "http4s-dsl" % "0.23.30",
      "org.http4s" %%% "http4s-ember-client" % "0.23.30",
      "org.http4s" %%% "http4s-ember-server" % "0.23.30",
      "org.http4s" %%% "http4s-server" % "0.23.30",
      "org.typelevel" %% "cats-core" % "2.13.0",
      "org.typelevel" %%% "cats-effect" % "3.6.3",
      "org.typelevel" %%% "cats-effect-kernel" % "3.6.3",
      "org.typelevel" %%% "cats-effect-std" % "3.6.3",
      "org.typelevel" %%% "case-insensitive" % "1.4.2"
    ),
    buildInfoKeys := Seq(version),
    buildInfoPackage := "plutus",
    nativeLinkingOptions ++= Seq(
      "-framework",
      "CoreFoundation",
      "-framework",
      "Security"
    ),
    nativeConfig ~= (conf =>
      if (sys.env.get("SN_RELEASE").contains("size"))
        conf.withMode(scala.scalanative.build.Mode.releaseSize)
      else
        conf
    ),
    crossPaths := false
  )
