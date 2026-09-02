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

lazy val keychain = projectMatrix
  .settings(
    dependencyUpdatesFailBuild := true,
    libraryDependencies += "org.typelevel" %%% "cats-effect" % "3.7.0"
  )
  .jvmPlatform(
    scalaVersions = scala3Versions,
    // `jvmPlatform` already prepends `VirtualAxis.jvm`.
    axisValues = Seq.empty,
    // `JextractPlugin` is enabled on the JVM row only: the native row's
    // bindings come from sn-bindgen instead, and each plugin's codegen would
    // otherwise run on a row that has no use for its output.
    configure = _.enablePlugins(JextractPlugin)
      .settings(
        // Emptied for Test, here and in the three other FFI rows: both
        // JextractPlugin and BindgenPlugin register their codegen under
        // `Compile` *and* `Test`, each reading `<config> / *Bindings`. Nothing
        // consumes the Test-scope output — there are no test sources — so all
        // it does is regenerate every binding a second time, concurrently with
        // the Compile run and, for the two rows that generate their header
        // below, writing that header while the other process is reading it.
        //
        // `sbt scalafixAll` (which touches both configs) has been seen to die
        // that way: sn-bindgen exits 10, which is Scala Native's
        // unhandled-exception code, having printed "Unrecoverable
        // NullPointerException in user thread" rather than any diagnostic about
        // the headers. It is intermittent — it reproduced on the first two runs
        // and then stopped reproducing, including from a cleaned src_managed —
        // so treat the race as the motivation for dropping duplicate work, not
        // as a diagnosis anyone has confirmed.
        //
        // It has to be this explicit override rather than scoping the
        // definition below to `Compile`: Test extends Compile in sbt's
        // configuration hierarchy, so `Test / *Bindings` delegates to the
        // Compile value and a Compile-scoped definition is still visible to
        // Test.
        Test / jextractBindings := Seq.empty,
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
              "CoreFoundation/CFData.h",
              "CoreFoundation/CFDictionary.h",
              "CoreFoundation/CFString.h",
              "Security/SecBase.h",
              "Security/SecItem.h"
            ).map(p => s"#include <$p>\n").mkString
          )
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
        // Emit MethodParameters into the jextract-generated Java bytecode so
        // `Keychain.scala` can call those methods with named arguments.
        javacOptions += "-parameters"
      )
  )
  .nativePlatform(
    scalaVersions = scala3Versions,
    // `nativePlatform` already prepends `VirtualAxis.native`.
    axisValues = Seq.empty,
    configure = _.enablePlugins(BindgenPlugin)
      .settings(
        // Emptied for Test for the reason given on the JVM row above.
        Test / bindgenBindings := Seq.empty,
        bindgenBindings += {
          // sn-bindgen filters out declarations from headers that clang tags as
          // system headers. Includes via the angle-bracket form (e.g.
          // `<CoreFoundation/CFNumber.h>`) get that tag; absolute-path includes
          // do not. So macos.h is generated at build time with the SDK path
          // (resolved via `xcrun --show-sdk-path`) baked in, rather than
          // hardcoding it.
          //
          // The documented escape hatch is `--exclude-system-path`
          // (`Binding.addExcludedSystemPath`), but on macOS it has no effect:
          // `loc.isFromSystemHeader` short-circuits the exclude check in
          // sn-bindgen's `ClangVisitor.scala`. Tracked upstream as
          // indoorvivants/sn-bindgen#361.
          val sdkPath = sys.process.Process("xcrun --show-sdk-path").!!.trim
          val header = (Compile / sourceManaged).value / "macos.h"
          IO.write(
            header,
            Seq(
              "CoreFoundation.framework/Versions/A/Headers/CFBase.h",
              "CoreFoundation.framework/Versions/A/Headers/CFNumber.h",
              "CoreFoundation.framework/Versions/A/Headers/CFData.h",
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
  .jvmPlatform(
    scalaVersions = scala3Versions,
    // `jvmPlatform` already prepends `VirtualAxis.jvm`.
    axisValues = Seq.empty,
    configure = _.enablePlugins(JextractPlugin)
      .settings(
        // Emptied for Test for the reason given in `keychain`.
        Test / jextractBindings := Seq.empty,
        jextractBindings += {
          val sdkPath = sys.process.Process("xcrun --show-sdk-path").!!.trim
          val header = (Compile / sourceManaged).value / "libsqlite.h"
          IO.write(header, s"#include <$sdkPath/usr/include/sqlite3.h>\n")
          JextractBinding(header, "libsqlite")
            .withArgs(
              Seq(
                "-l",
                ":/usr/lib/libsqlite3.dylib"
              )
            )
        },
        jextractMode := JextractMode.ResourceGenerator,
        // Emit MethodParameters into the jextract-generated Java bytecode so
        // `Sqlite.scala` can call those methods with named arguments.
        javacOptions += "-parameters"
      )
  )
  .nativePlatform(
    scalaVersions = scala3Versions,
    // `nativePlatform` already prepends `VirtualAxis.native`.
    axisValues = Seq.empty,
    // `VcpkgNativePlugin` (which auto-loads `ScalaNativePlugin`) is enabled on
    // the native row only, for the reason spelled out on `main` below.
    configure = _.enablePlugins(BindgenPlugin, VcpkgNativePlugin)
      .settings(
        vcpkgDependencies := VcpkgDependencies("sqlite3"),
        // Emptied for Test for the reason given in `keychain`.
        Test / bindgenBindings := Seq.empty,
        bindgenBindings += {
          // Package `libsqlite` (not `sqlite3`) avoids colliding with the
          // `sqlite3` struct that lives inside it.
          bindgen.interface
            .Binding(
              vcpkgConfigurator.value.includes("sqlite3") / "sqlite3.h",
              "libsqlite"
            )
            .addCImport("sqlite3.h")
            // Opt in to `#define` macro rendering for the SQLite result codes
            // and open flags we use (`SQLITE_OK`, `SQLITE_ROW`,
            // `SQLITE_OPEN_*`, …). `onlyValidMacros` skips the ones with
            // composite expressions (e.g. `SQLITE_OK_LOAD_PERMANENTLY
            // (SQLITE_OK | (1<<8))`) instead of erroring out.
            .withMacros(Set("SQLITE_*"))
            .withOnlyValidMacros(true)
            .withLogLevel(bindgen.interface.LogLevel.Info)
        },
        tpolecatExcludeOptions ++= Set(
          ScalacOptions.deprecation,
          ScalacOptions.warnUnusedImports
        )
      )
  )

lazy val main = projectMatrix
  .enablePlugins(BuildInfoPlugin, Smithy4sCodegenPlugin)
  .dependsOn(keychain, porcupine)
  .settings(
    dependencyUpdatesFailBuild := true,
    libraryDependencies ++= Seq(
      "co.fs2" %%% "fs2-io" % "3.13.0",
      "com.disneystreaming.smithy4s" %%% "smithy4s-core" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %%% "smithy4s-http4s" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %%% "smithy4s-json" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %%% "smithy4s-xml" % smithy4sVersion.value,
      "com.lihaoyi" %%% "fansi" % "0.5.1",
      "com.monovore" %%% "decline-effect" % "2.6.2",
      "org.http4s" %%% "http4s-ember-client" % "0.23.34",
      "org.http4s" %%% "http4s-ember-server" % "0.23.34",
      "tech.neander" %%% "cue4s" % "0.0.12"
    ),
    buildInfoKeys := Seq[BuildInfoKey](
      libraryDependencies,
      sbtVersion,
      scalaVersion,
      version
    ),
    buildInfoOptions ++= Seq(BuildInfoOption.BuildTime, BuildInfoOption.ToMap),
    buildInfoPackage := "plutus"
  )
  .jvmPlatform(
    scalaVersions = scala3Versions,
    // `jvmPlatform` already prepends `VirtualAxis.jvm`.
    axisValues = Seq.empty,
    configure = _.settings(
      libraryDependencies += "org.slf4j" % "slf4j-simple" % "2.0.17",
      fork := true,
      connectInput := true,
      // FFM API is final in JDK 22+; suppress the runtime "restricted method"
      // warning so stderr stays clean during `main3/run`.
      javaOptions += "--enable-native-access=ALL-UNNAMED"
    )
  )
  .nativePlatform(
    scalaVersions = scala3Versions,
    // `nativePlatform` already prepends `VirtualAxis.native`.
    axisValues = Seq.empty,
    // `VcpkgNativePlugin` (auto-loads `ScalaNativePlugin`) is enabled here on
    // the native row only, not at the projectMatrix level. The native plugin
    // hijacks `%%%` cross-version resolution and adds nscplugin to every row
    // it's applied to, so enabling it project-wide poisons JVM compilation.
    configure = _.enablePlugins(VcpkgNativePlugin)
      .settings(
        vcpkgDependencies := VcpkgDependencies("sqlite3"),
        // Append rather than replace: VcpkgNativePlugin has already injected
        // `-L<vcpkg-install>/lib -lsqlite3 -pthread`, which a bare
        // `withLinkingOptions(Seq(...))` would discard.
        nativeConfig ~= (nativeConfig =>
          nativeConfig.withLinkingOptions(
            nativeConfig.linkingOptions ++ Seq(
              "-framework",
              "CoreFoundation",
              "-framework",
              "Security",
              // Homebrew install path for s2n, pulled in by epollcat for TLS.
              "-L/opt/homebrew/lib"
            )
          )
        )
      )
  )

lazy val scala3Versions = Seq(scala3Version)

lazy val scala3Version = "3.8.3"
