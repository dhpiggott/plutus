name := "plutus"

Global / onChangedBuildSource := ReloadOnSourceChanges

addCommandAlias("fmtCheck", "all scalafmtSbtCheck scalafmtCheckAll")
addCommandAlias("fmt", "all scalafmtSbt scalafmtAll")
addCommandAlias("fixCheck", "scalafixAll --check")
addCommandAlias("fix", "scalafixAll")
addCommandAlias(
  "check",
  "fmtCheck; fixCheck; undeclaredCompileDependenciesTest; unusedCompileDependenciesTest; missinglinkCheck; dependencyUpdates"
)

inThisBuild(
  Seq(
    scalaVersion := "3.6.2",
    semanticdbEnabled := true
  )
)

lazy val plutus = project
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    dependencyUpdatesFailBuild := true,
    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-core" % "3.11.0",
      "co.fs2" %% "fs2-io" % "3.11.0",
      "com.disneystreaming.smithy4s" %% "smithy4s-cats" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s-kernel" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-json" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-xml" % smithy4sVersion.value,
      "com.monovore" %% "decline" % "2.5.0",
      "com.monovore" %% "decline-effect" % "2.5.0",
      "org.gnieh" %% "fs2-data-text" % "1.11.2",
      "org.gnieh" %% "fs2-data-xml" % "1.11.2",
      "org.http4s" %% "http4s-client" % "0.23.30",
      "org.http4s" %% "http4s-core" % "0.23.30",
      "org.http4s" %% "http4s-dsl" % "0.23.30",
      "org.http4s" %% "http4s-ember-client" % "0.23.30",
      "org.http4s" %% "http4s-ember-server" % "0.23.30",
      "org.http4s" %% "http4s-server" % "0.23.30",
      "org.slf4j" % "slf4j-simple" % "2.0.16" % Runtime,
      "org.typelevel" %% "cats-core" % "2.13.0",
      "org.typelevel" %% "cats-effect" % "3.5.7",
      "org.typelevel" %% "cats-effect-kernel" % "3.5.7",
      "org.typelevel" %% "cats-effect-std" % "3.5.7",
      "org.typelevel" %% "case-insensitive" % "1.4.2"
    ),
    missinglinkIgnoreDestinationPackages += IgnoredPackage("jnr.unixsocket"),
    missinglinkIgnoreSourcePackages += IgnoredPackage("org.slf4j"),
    Compile / run / connectInput := true,
    Compile / run / fork := true,
    Compile / run / outputStrategy := Some(StdoutOutput)
  )
