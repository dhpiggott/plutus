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
      "org.typelevel" %%% "cats-effect" % "3.7.0",
      "com.lihaoyi" %%% "fansi" % "0.5.1"
    )
  )
  .jvmPlatform(scalaVersions = scalaVersions)
  .nativePlatform(scalaVersions = scalaVersions)

// TODO: Change from noop to use Java FFI.
lazy val `jvm-noop-state-store` = projectMatrix
  .dependsOn(`smithy4s-schemas`)
  .settings(dependencyUpdatesFailBuild := true)
  .jvmPlatform(
    scalaVersions = scalaVersions,
    Seq(
      libraryDependencies += "org.typelevel" %%% "cats-effect" % "3.7.0"
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
      "org.typelevel" %%% "cats-effect" % "3.6.3",
      "co.fs2" %%% "fs2-core" % "3.12.2",
      "org.scodec" %%% "scodec-bits" % "1.2.1"
    )
  )
  .jvmPlatform(scalaVersions = scalaVersions)

lazy val `porcupine-jvm` = projectMatrix
  .dependsOn(porcupine)
  .settings(
    dependencyUpdatesFailBuild := true,
    libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.47.1.0"
  )
  .jvmPlatform(scalaVersions = scalaVersions)

// TODO: Use sn-bindgen where possible.
lazy val `porcupine-native` = projectMatrix
  .settings(
    dependencyUpdatesFailBuild := true,
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-effect" % "3.6.3",
      "co.fs2" %%% "fs2-core" % "3.9.4",
      "org.scodec" %%% "scodec-bits" % "1.1.38"
    )
  )
  .nativePlatform(scalaVersions = scalaVersions)

lazy val `jvm-gnucash` = projectMatrix
  .dependsOn(`smithy4s-schemas`, log, `porcupine-jvm`)
  .settings(
    dependencyUpdatesFailBuild := true
  )
  .jvmPlatform(
    scalaVersions = scalaVersions,
    Seq(
      libraryDependencies ++= Seq(
        "co.fs2" %%% "fs2-io" % "3.13.0",
        "com.monovore" %%% "decline-effect" % "2.6.2",
        "tech.neander" %%% "cue4s" % "0.0.11"
      )
    )
  )

// TODO: Merge with JVM version.
lazy val `native-noop-gnucash` = projectMatrix
  .dependsOn(`smithy4s-schemas`)
  .settings(dependencyUpdatesFailBuild := true)
  .nativePlatform(
    scalaVersions = scalaVersions,
    Seq(
      libraryDependencies += "com.monovore" %%% "decline" % "2.6.2"
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
      "com.monovore" %%% "decline-effect" % "2.6.2",
      "org.http4s" %%% "http4s-ember-client" % "0.23.34",
      "org.http4s" %%% "http4s-ember-server" % "0.23.34"
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
      excludeDependencies ++= Seq(
        ExclusionRule("org.scala-native"),
        ExclusionRule("org.portable-scala")
      ),
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
        // TODO: Use different tasks?
        (if (sys.env.get("SN_RELEASE").contains("size"))
           conf.withMode(scala.scalanative.build.Mode.releaseSize)
         else
           conf).withLinkingOptions(
          Seq(
            "-framework",
            "CoreFoundation",
            "-framework",
            "Security",
            "-lsqlite3",
            // TODO: Review to see if this is really necessary.
            "-L/opt/homebrew/lib"
          )
        )
      ),
      crossPaths := false
    )
  )

lazy val scalaVersions = Seq(scalaVersion)

lazy val scalaVersion = "3.8.3"
