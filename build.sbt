import org.typelevel.scalacoptions.ScalacOptions

Global / onChangedBuildSource := ReloadOnSourceChanges

name := "plutus"

inThisBuild(
  Seq(
    semanticdbEnabled := true
  )
)

lazy val `smithy4s-schemas` = projectMatrix
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    dependencyUpdatesFailBuild := true,
    libraryDependencies += "com.disneystreaming.smithy4s" %%% "smithy4s-core" % smithy4sVersion.value
  )
  .jvmPlatform(scalaVersions = scalaVersions)
  .nativePlatform(scalaVersions = scalaVersions)

lazy val `log` = projectMatrix
  .dependsOn(`smithy4s-schemas`)
  .settings(
    dependencyUpdatesFailBuild := true,
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-effect" % "3.6.3",
      "com.lihaoyi" %%% "fansi" % "0.4.0"
    )
  )
  .jvmPlatform(scalaVersions = scalaVersions)
  .nativePlatform(scalaVersions = scalaVersions)

lazy val `jvm-noop-state-store` = projectMatrix
  .dependsOn(`smithy4s-schemas`)
  .settings(dependencyUpdatesFailBuild := true)
  .jvmPlatform(
    scalaVersions = scalaVersions,
    Seq(
      libraryDependencies += "org.typelevel" %%% "cats-effect" % "3.6.3"
    )
  )

lazy val `native-macos-keychain-state-store` = projectMatrix
  .dependsOn(`smithy4s-schemas`, log)
  .enablePlugins(BindgenPlugin)
  .settings(dependencyUpdatesFailBuild := true)
  .nativePlatform(
    scalaVersions = scalaVersions,
    Seq(
      bindgenVersion := "0.3.1",
      bindgenBindings += bindgen.interface
        .Binding(
          (Compile / sourceDirectory).value / "include" / "macos.h",
          "macos"
        )
        .addCImport("CoreFoundation/CFString.h")
        .withLogLevel(bindgen.interface.LogLevel.Info),
      libraryDependencies ++= Seq(
        "com.disneystreaming.smithy4s" %%% "smithy4s-json" % smithy4sVersion.value,
        "org.typelevel" %%% "cats-effect" % "3.6.3"
      ),
      tpolecatExcludeOptions ++= Set(
        ScalacOptions.deprecation,
        ScalacOptions.warnUnusedImports
      )
    )
  )

lazy val `jvm-gnucash` = projectMatrix
  .dependsOn(`smithy4s-schemas`, log)
  .settings(
    dependencyUpdatesFailBuild := true
  )
  .jvmPlatform(
    scalaVersions = scalaVersions,
    Seq(
      libraryDependencies ++= Seq(
        "co.fs2" %%% "fs2-io" % "3.12.2",
        "com.armanbilge" %%% "porcupine" % "0.0.1",
        "com.monovore" %%% "decline-effect" % "2.4.1",
        "tech.neander" %%% "cue4s" % "0.0.10"
      )
    )
  )

lazy val `native-noop-gnucash` = projectMatrix
  .dependsOn(`smithy4s-schemas`)
  .settings(dependencyUpdatesFailBuild := true)
  .nativePlatform(
    scalaVersions = scalaVersions,
    Seq(
      libraryDependencies += "com.monovore" %%% "decline" % "2.4.1"
    )
  )

lazy val main = projectMatrix
  .dependsOn(`smithy4s-schemas`, log)
  .enablePlugins(BuildInfoPlugin, JavaAppPackaging)
  .settings(
    dependencyUpdatesFailBuild := true,
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %%% "smithy4s-http4s" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %%% "smithy4s-xml" % smithy4sVersion.value,
      "com.monovore" %%% "decline-effect" % "2.4.1",
      "org.http4s" %%% "http4s-ember-client" % "0.23.33",
      "org.http4s" %%% "http4s-ember-server" % "0.23.33"
    ),
    buildInfoKeys := Seq(version),
    buildInfoPackage := "plutus"
  )
  .dependsOn(
    `jvm-noop-state-store`.jvm(scalaVersion),
    `jvm-gnucash`.jvm(scalaVersion)
  )
  .jvmPlatform(
    scalaVersions = scalaVersions,
    Seq(
      libraryDependencies += "org.slf4j" % "slf4j-simple" % "2.0.17",
      connectInput := true,
      fork := true
    )
  )
  .dependsOn(
    `native-macos-keychain-state-store`.native(scalaVersion),
    `native-noop-gnucash`.native(scalaVersion)
  )
  .nativePlatform(
    scalaVersions = scalaVersions,
    Seq(
      nativeConfig ~= (conf =>
        if (sys.env.get("SN_RELEASE").contains("size"))
          conf.withMode(scala.scalanative.build.Mode.releaseSize)
        else
          conf
      ),
      nativeLinkingOptions ++= Seq(
        "-framework",
        "CoreFoundation",
        "-framework",
        "Security",
        "-lsqlite3"
      ),
      crossPaths := false
    )
  )

lazy val scalaVersions = Seq(scalaVersion)

lazy val scalaVersion = "3.7.4"
