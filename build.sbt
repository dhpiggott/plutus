import org.typelevel.scalacoptions.ScalacOptions

Global / onChangedBuildSource := ReloadOnSourceChanges

name := "plutus"

scalaVersion := scala3Version

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
  .jvmPlatform(scalaVersions = scala3Versions)
  .nativePlatform(scalaVersions = scala3Versions)

lazy val `log` = projectMatrix
  .dependsOn(`smithy4s-schemas`)
  .settings(
    dependencyUpdatesFailBuild := true,
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-effect" % "3.7.0",
      "com.lihaoyi" %%% "fansi" % "0.5.1"
    )
  )
  .jvmPlatform(scalaVersions = scala3Versions)
  .nativePlatform(scalaVersions = scala3Versions)

// TODO: Change from noop to use Java FFI.
lazy val `jvm-noop-state-store` = projectMatrix
  .dependsOn(`smithy4s-schemas`)
  .settings(dependencyUpdatesFailBuild := true)
  .jvmPlatform(
    scalaVersions = scala3Versions,
    Seq(
      libraryDependencies += "org.typelevel" %%% "cats-effect" % "3.7.0"
    )
  )

lazy val `native-macos-keychain-state-store` = projectMatrix
  .dependsOn(`smithy4s-schemas`, log)
  .enablePlugins(BindgenPlugin)
  .settings(dependencyUpdatesFailBuild := true)
  .nativePlatform(
    scalaVersions = scala3Versions,
    Seq(
      bindgenBindings += {
        // sn-bindgen filters out declarations from headers that clang tags as
        // system headers. Includes via the angle-bracket form (e.g.
        // `<CoreFoundation/CFNumber.h>`) get that tag; absolute-path includes
        // do not. So macos.h is generated at build time with the SDK path
        // (resolved via `xcrun --show-sdk-path`) baked in, rather than
        // hardcoding it.
        //
        // TODO: Check if this is an upstream bug and if so if it can be fixed.
        val sdkPath = sys.process.Process("xcrun --show-sdk-path").!!.trim
        val header = (Compile / sourceManaged).value / "macos.h"
        IO.write(
          header,
          Seq(
            "CoreFoundation.framework/Versions/A/Headers/CFNumber.h",
            "CoreFoundation.framework/Versions/A/Headers/CFDictionary.h",
            "CoreFoundation.framework/Versions/A/Headers/CFString.h",
            "Security.framework/Versions/A/Headers/SecBase.h",
            "Security.framework/Versions/A/Headers/SecItem.h"
          ).map(p => s"#include <$sdkPath/System/Library/Frameworks/$p>\n")
            .mkString
        )
        bindgen.interface
          .Binding(header, "macos")
          .addCImport("CoreFoundation/CFString.h")
          .withLogLevel(bindgen.interface.LogLevel.Info)
      },
      libraryDependencies ++= Seq(
        "com.disneystreaming.smithy4s" %%% "smithy4s-json" % smithy4sVersion.value,
        "org.typelevel" %%% "cats-effect" % "3.7.0"
      ),
      tpolecatExcludeOptions ++= Set(
        ScalacOptions.deprecation,
        ScalacOptions.warnUnusedImports
      )
    )
  )

lazy val porcupine = projectMatrix
  .settings(
    dependencyUpdatesFailBuild := true,
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-effect" % "3.7.0",
      "co.fs2" %%% "fs2-core" % "3.13.0",
      "org.scodec" %%% "scodec-bits" % "1.2.4"
    )
  )
  .jvmPlatform(scalaVersions = scala3Versions)
  .nativePlatform(scalaVersions = scala3Versions)

lazy val `porcupine-jvm` = projectMatrix
  .dependsOn(porcupine)
  .settings(
    dependencyUpdatesFailBuild := true,
    libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.53.0.0"
  )
  .jvmPlatform(scalaVersions = scala3Versions)

// TODO: Use sn-bindgen where possible.
lazy val `porcupine-native` = projectMatrix
  .dependsOn(porcupine)
  .settings(dependencyUpdatesFailBuild := true)
  .nativePlatform(scalaVersions = scala3Versions)

lazy val main = projectMatrix
  .dependsOn(`smithy4s-schemas`, log)
  .enablePlugins(BuildInfoPlugin, JavaAppPackaging)
  .settings(
    dependencyUpdatesFailBuild := true,
    libraryDependencies ++= Seq(
      "co.fs2" %%% "fs2-io" % "3.13.0",
      "com.disneystreaming.smithy4s" %%% "smithy4s-http4s" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %%% "smithy4s-xml" % smithy4sVersion.value,
      "com.monovore" %%% "decline-effect" % "2.6.2",
      "org.http4s" %%% "http4s-ember-client" % "0.23.34",
      "org.http4s" %%% "http4s-ember-server" % "0.23.34",
      "tech.neander" %%% "cue4s" % "0.0.12"
    ),
    buildInfoKeys := Seq(version),
    buildInfoPackage := "plutus"
  )
  .dependsOn(`jvm-noop-state-store`.jvm(scala3Version))
  .dependsOn(`porcupine-jvm`.jvm(scala3Version))
  .jvmPlatform(
    scalaVersions = scala3Versions,
    Seq(
      // The .native(...) projectMatrix dependsOn calls below leak Scala Native
      // transitives (scalalib_native0.5_2.13) into the JVM resolve graph;
      // exclude them here to avoid cross-version-suffix conflicts.
      excludeDependencies ++= Seq(
        ExclusionRule("org.scala-native"),
        ExclusionRule("org.portable-scala")
      ),
      libraryDependencies += "org.slf4j" % "slf4j-simple" % "2.0.17",
      connectInput := true,
      fork := true
    )
  )
  .dependsOn(`native-macos-keychain-state-store`.native(scala3Version))
  .dependsOn(`porcupine-native`.native(scala3Version))
  .nativePlatform(
    scalaVersions = scala3Versions,
    Seq(
      nativeConfig ~= (_.withLinkingOptions(
        Seq(
          "-framework",
          "CoreFoundation",
          "-framework",
          "Security",
          "-lsqlite3",
          // Homebrew install path for s2n, pulled in by epollcat for TLS.
          "-L/opt/homebrew/lib"
        )
      )),
      crossPaths := false
    )
  )

lazy val scala3Versions = Seq(scala3Version)

lazy val scala3Version = "3.8.3"
