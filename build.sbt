addCommandAlias("fmtCheck", "all scalafmtSbtCheck scalafmtCheckAll")
addCommandAlias("fmt", "all scalafmtSbt scalafmtAll")
addCommandAlias("fixCheck", "scalafixAll --check")
addCommandAlias("fix", "scalafixAll")
addCommandAlias("check", "fmtCheck; fixCheck; dependencyUpdates")

Global / onChangedBuildSource := ReloadOnSourceChanges

name := "plutus"

inThisBuild(
  Seq(
    scalaVersion := "3.6.2",
    semanticdbEnabled := true
  )
)

lazy val keychain = project
  .enablePlugins(
    BindgenPlugin
  )
  .settings(
    dependencyUpdatesFailBuild := true,
    bindgenMode := bindgen.plugin.BindgenMode.Manual(
      scalaDir = baseDirectory.value,
      cDir = baseDirectory.value
    ),
    bindgenBindings ++= Seq(
      bindgen.interface.Binding(
        file(
          // TODO: See https://github.com/scala-native/scala-native/issues/2875?
          "/Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h"
        ),
        "cfbase"
      ),
      bindgen.interface.Binding(
        file(
          // TODO:
          "/Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h"
        ),
        "cfdictionary"
      ),
      bindgen.interface.Binding(
        file(
          // TODO:
          "/Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFNumber.h"
        ),
        "cfnumber"
      ),
      bindgen.interface.Binding(
        file(
          // TODO:
          "/Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h"
        ),
        "cfstring"
      ),
      bindgen.interface.Binding(
        file(
          // TODO:
          "/Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecItem.h"
        ),
        "secitem"
      )
    ),
    tpolecatExcludeOptions ++= Set(
      // TODO:
      org.typelevel.scalacoptions.ScalacOptions.deprecation,
      org.typelevel.scalacoptions.ScalacOptions.warnUnusedImports
    )
  )

lazy val plutus = project
  .dependsOn(keychain)
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
    ),
    buildInfoKeys := Seq(version),
    buildInfoPackage := "plutus",
    crossPaths := false,
    // TODO:
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
    )
  )
