name := "plutus"

Global / onChangedBuildSource := ReloadOnSourceChanges

inThisBuild(
  Seq(
    scalaVersion := "3.6.2",
    semanticdbEnabled := true
  )
)
addCommandAlias("fmt", "all scalafmtSbt scalafmtAll")
addCommandAlias("fix", "scalafixAll")

lazy val plutus = project
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-xml" % smithy4sVersion.value,
      // TODO: Switch to Cats Effect?
      "dev.zio" %% "zio" % "2.1.14",
      "dev.zio" %% "zio-interop-cats" % "23.1.0.3",
      "dev.zio" %% "zio-stacktracer" % "2.1.14",
      "org.gnieh" %% "fs2-data-xml" % "1.11.2",
      "org.http4s" %% "http4s-client" % "0.23.30",
      "org.http4s" %% "http4s-core" % "0.23.30",
      "org.http4s" %% "http4s-dsl" % "0.23.30",
      "org.http4s" %% "http4s-ember-client" % "0.23.30",
      "org.http4s" %% "http4s-ember-server" % "0.23.30",
      "org.typelevel" %% "case-insensitive" % "1.4.2",
      "org.typelevel" %% "cats-effect" % "3.5.7",
      "org.typelevel" %% "cats-effect-kernel" % "3.5.7",
      "org.slf4j" % "slf4j-simple" % "2.0.16" % Runtime
    ),
    run / connectInput := true
  )
