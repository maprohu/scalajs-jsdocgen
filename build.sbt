
val commonSettings = Seq(
  organization := "com.github.maprohu",
  version := "0.1.0"
)

lazy val plugin = project
  .settings(commonSettings)
  .settings(
    scalaVersion := "2.10.6",
    sbtPlugin := true,
    name := "jsdocgen-plugin",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "upickle" % "0.3.6",
      "org.scalamacros" %% s"quasiquotes" % "2.0.0" % "provided"
    )

  )


lazy val lib = project
  .settings(commonSettings)
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "jsdocgen-lib",
    scalaVersion := "2.11.7"
  )
