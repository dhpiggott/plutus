name := "plutus"

Global / onChangedBuildSource := ReloadOnSourceChanges

inThisBuild(
  Seq(
    scalaVersion := "3.2.1",
    scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.5.0",
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
      "dev.zio" %% "zio" % "2.0.6",
      "dev.zio" %% "zio-interop-cats" % "23.0.0.1",
      "dev.zio" %% "zio-stacktracer" % "2.0.6",
      "dev.zio" %% "zio-nio" % "2.0.1",
      "org.gnieh" %% "fs2-data-xml" % "1.5.1",
      "org.http4s" %% "http4s-client" % "0.23.18",
      "org.http4s" %% "http4s-core" % "0.23.18",
      "org.http4s" %% "http4s-dsl" % "0.23.18",
      "org.http4s" %% "http4s-ember-client" % "0.23.18",
      "org.http4s" %% "http4s-ember-server" % "0.23.18",
      "org.typelevel" %% "case-insensitive" % "1.3.0",
      "org.typelevel" %% "cats-effect" % "3.4.5",
      "org.typelevel" %% "cats-effect-kernel" % "3.4.5",
      "org.slf4j" % "slf4j-simple" % "2.0.6" % Runtime
    ),
    run / connectInput := true
  )
