val githubRepo = "scalajs-jsdocgen"

val jacksonVersion = "2.6.3"

val commonSettings = Seq(
  organization := "com.github.maprohu",
  version := "0.1.2-SNAPSHOT",
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
  publishArtifact := false,
  publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))
)

lazy val plugin = project
  .settings(commonSettings)
  .settings(
    scalaVersion := "2.10.6",
    sbtPlugin := true,
    name := "jsdocgen-plugin",
    publishArtifact in (Compile, packageDoc) := false,
    addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.5"),
    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
      "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
      "com.lihaoyi" %% "upickle" % "0.3.6",
      "org.scalamacros" %% "quasiquotes" % "2.0.0" % "provided"
    ),
    resourceGenerators in Compile += Def.task {
      val out = (resourceManaged in Compile).value / "jsdocgen" / "plugin.properties"
      IO.write(out,
      s"""organization = ${organization.value}
          |lib.name = ${(name in lib).value}
          |version = ${version.value}
      """.stripMargin
      )
      Seq(out)
    }.taskValue


  )


lazy val lib = project
  .settings(commonSettings)
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "jsdocgen-lib",
    publishArtifact in (Compile, packageDoc) := false,
    scalaVersion := "2.11.7"
  )


lazy val root = (project in file("."))
  .settings(noPublish)
  .aggregate(plugin, lib)
