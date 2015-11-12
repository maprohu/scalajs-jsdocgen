val githubRepo = "scalajs-jsdocgen"

val commonSettings = Seq(
  organization := "com.github.maprohu",
  version := "0.1.0-SNAPSHOT",
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  pomIncludeRepository := { _ => false },
  licenses := Seq("BSD-style" -> url("http://www.opensource.org/licenses/bsd-license.php")),
  homepage := Some(url(s"https://github.com/maprohu/${githubRepo}")),
  pomExtra := (
    <scm>
      <url>git@github.com:maprohu/{githubRepo}.git</url>
      <connection>scm:git:git@github.com:maprohu/{githubRepo}.git</connection>
    </scm>
      <developers>
        <developer>
          <id>maprohu</id>
          <name>maprohu</name>
          <url>https://github.com/maprohu</url>
        </developer>
      </developers>
    )
)

val noPublish = Seq(
  publishArtifact := false
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


lazy val root = (project in file("."))
  .settings(noPublish)
  .aggregate(plugin, lib)
