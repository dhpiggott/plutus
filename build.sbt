import org.typelevel.scalacoptions.ScalacOptions
import sbt_jextract.*

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

lazy val `macos-keychain-state-store-jvm` = projectMatrix
  .dependsOn(`smithy4s-schemas`, log)
  .enablePlugins(JextractPlugin)
  .settings(
    dependencyUpdatesFailBuild := true,
    jextractBindings += {
      val sdkPath = sys.process.Process("xcrun --show-sdk-path").!!.trim
      val managed = (Compile / sourceManaged).value
      val includeDir = managed / "include"
      IO.createDirectory(includeDir)
      Seq("CoreFoundation", "Security").foreach { fw =>
        val link = (includeDir / fw).toPath
        val target = file(
          s"$sdkPath/System/Library/Frameworks/$fw.framework/Headers"
        ).toPath
        if (!java.nio.file.Files.exists(link))
          java.nio.file.Files.createSymbolicLink(link, target)
      }
      val header = managed / "macos.h"
      IO.write(
        header,
        Seq(
          "CoreFoundation/CFNumber.h",
          "CoreFoundation/CFDictionary.h",
          "CoreFoundation/CFString.h",
          "Security/SecBase.h",
          "Security/SecItem.h"
        ).map(p => s"#include <$p>\n").mkString
      )
      // TODO: Switch back to a dotted package once
      // https://github.com/indoorvivants/sbt-jextract/issues/1 is fixed.
      // sbt-jextract's source generator does `IO.listFiles(dest / pkg)`,
      // passing the package name literally as a directory segment — so a
      // dotted package like "plutus.macos" looks for `dest/plutus.macos/`,
      // while jextract actually creates `dest/plutus/macos/`. The generator
      // returns an empty Set, and `sbt clean compile` only picks the Java
      // sources up on the second run (via unmanagedSourceDirectories). A
      // single-segment package avoids the mismatch.
      JextractBinding(header, "macos")
        .withArgs(
          Seq(
            "-I",
            includeDir.getAbsolutePath,
            "-l",
            ":/System/Library/Frameworks/CoreFoundation.framework/CoreFoundation",
            "-l",
            ":/System/Library/Frameworks/Security.framework/Security"
          )
        )
    },
    jextractMode := JextractMode.ResourceGenerator,
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %%% "smithy4s-json" % smithy4sVersion.value,
      "org.typelevel" %%% "cats-effect" % "3.7.0"
    )
  )
  .jvmPlatform(scalaVersions = scala3Versions)

lazy val `macos-keychain-state-store-native` = projectMatrix
  .dependsOn(`smithy4s-schemas`, log)
  .enablePlugins(BindgenPlugin)
  .settings(
    dependencyUpdatesFailBuild := true,
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
  .nativePlatform(scalaVersions = scala3Versions)

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

lazy val `porcupine-native` = projectMatrix
  .dependsOn(porcupine)
  .enablePlugins(BindgenPlugin, VcpkgNativePlugin)
  .settings(
    dependencyUpdatesFailBuild := true,
    vcpkgDependencies := VcpkgDependencies("sqlite3"),
    bindgenBindings += {
      // Package `libsqlite` (not `sqlite3`) avoids colliding with the
      // `sqlite3` struct that lives inside it.
      bindgen.interface
        .Binding(
          vcpkgConfigurator.value.includes("sqlite3") / "sqlite3.h",
          "libsqlite"
        )
        .addCImport("sqlite3.h")
        // Opt in to `#define` macro rendering for the SQLite result codes and
        // open flags we use (`SQLITE_OK`, `SQLITE_ROW`, `SQLITE_OPEN_*`, …).
        // `onlyValidMacros` skips the ones with composite expressions (e.g.
        // `SQLITE_OK_LOAD_PERMANENTLY (SQLITE_OK | (1<<8))`) instead of
        // erroring out.
        .withMacros(Set("SQLITE_*"))
        .withOnlyValidMacros(true)
        .withLogLevel(bindgen.interface.LogLevel.Info)
    },
    tpolecatExcludeOptions ++= Set(
      ScalacOptions.deprecation,
      ScalacOptions.warnUnusedImports
    )
  )
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
  .dependsOn(`macos-keychain-state-store-jvm`.jvm(scala3Version))
  .dependsOn(`porcupine-jvm`.jvm(scala3Version))
  .jvmPlatform(
    scalaVersions = scala3Versions,
    // `jvmPlatform` already prepends `VirtualAxis.jvm`.
    axisValues = Seq.empty,
    configure = _.settings(
      // The .native(...) projectMatrix dependsOn calls below leak Scala Native
      // transitives (scalalib_native0.5_2.13) into the JVM resolve graph;
      // exclude them here to avoid cross-version-suffix conflicts.
      excludeDependencies ++= Seq(
        ExclusionRule("org.scala-native"),
        ExclusionRule("org.portable-scala")
      ),
      libraryDependencies += "org.slf4j" % "slf4j-simple" % "2.0.17",
      connectInput := true,
      fork := true,
      // FFM API is final in JDK 22+; suppress the runtime "restricted method"
      // warning so stderr stays clean during `main3/run`.
      javaOptions += "--enable-native-access=ALL-UNNAMED"
    )
  )
  .dependsOn(`macos-keychain-state-store-native`.native(scala3Version))
  .dependsOn(`porcupine-native`.native(scala3Version))
  .nativePlatform(
    scalaVersions = scala3Versions,
    // `nativePlatform` already prepends `VirtualAxis.native`.
    axisValues = Seq.empty,
    // `VcpkgNativePlugin` (auto-loads `ScalaNativePlugin`) is enabled here on
    // the native row only, not at the projectMatrix level. The native plugin
    // hijacks `%%%` cross-version resolution and adds nscplugin to every row
    // it's applied to, so enabling it project-wide poisons JVM compilation.
    configure = _.enablePlugins(VcpkgNativePlugin).settings(
      vcpkgDependencies := VcpkgDependencies("sqlite3"),
      // Append rather than replace: VcpkgNativePlugin has already injected
      // `-L<vcpkg-install>/lib -lsqlite3 -pthread`, which a bare
      // `withLinkingOptions(Seq(...))` would discard.
      nativeConfig ~= (c =>
        c.withLinkingOptions(
          c.linkingOptions ++ Seq(
            "-framework",
            "CoreFoundation",
            "-framework",
            "Security",
            // Homebrew install path for s2n, pulled in by epollcat for TLS.
            "-L/opt/homebrew/lib"
          )
        )
      ),
      crossPaths := false
    )
  )

lazy val scala3Versions = Seq(scala3Version)

lazy val scala3Version = "3.8.3"
