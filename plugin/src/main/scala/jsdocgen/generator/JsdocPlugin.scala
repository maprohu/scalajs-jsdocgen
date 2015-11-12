package jsdocgen.generator

import sbt._
import Keys._

object JsdocPlugin extends AutoPlugin {

  override val projectSettings: Seq[Setting[_]] = baseNonameSettings

  object autoImport {
    lazy val jsdocGenerate = taskKey[Seq[File]]("jsdoc-generate")

    lazy val jsdocTarget = taskKey[File]("jsdoc-target")

    lazy val jsdocDocletsFile = settingKey[File]("jsdoc-docletsfile")

    lazy val jsdocRunTarget = settingKey[File]("jsdoc-runtarget")

    lazy val jsdocRunSource = settingKey[Option[URI]]("jsdoc-runsource")

    lazy val jsdocRunInputs = settingKey[Seq[String]]("jsdoc-runinputs")

    lazy val jsdocImplicits = settingKey[Seq[String]]("jsdoc-implicits")

    lazy val jsdocGlobalScope = settingKey[Seq[String]]("jsdoc-globalscope")

    lazy val jsdocUtilScope = settingKey[String]("jsdoc-utilscope")

    lazy val jsdocCommand = settingKey[Seq[String]]("jsdoc-command")

    lazy val jsdocRun = taskKey[File]("jsdoc-run")

  }

  import autoImport._

  lazy val baseNonameSettings: Seq[sbt.Def.Setting[_]] = Seq(
    jsdocCommand := {
      if (sys.props.get("os.name").exists(_.toLowerCase().contains("windows")))
        Seq("cmd", "/C", "jsdoc")
      else
        Seq("jsdoc")
    },
    jsdocTarget := (sourceManaged in Compile).value / "jsdocgen",
    jsdocGlobalScope := Seq("jsdocgen"),
    jsdocUtilScope := "pkg",
    jsdocImplicits := Seq("implicits"),
    jsdocDocletsFile := target.value / "jsdoc.json",
    jsdocRunTarget := jsdocDocletsFile.value,
    jsdocRunSource := None,
    jsdocRunInputs := Seq("."),
    jsdocGenerate := {
      Generator.generateFile(
        jsdocTarget.value,
        jsdocDocletsFile.value,
        jsdocGlobalScope.value,
        jsdocUtilScope.value,
        jsdocImplicits.value
      )
    },
    jsdocRun := {
      JsdocInvoker.run(
        jsdocRunSource.value.get,
        jsdocRunInputs.value,
        jsdocRunTarget.value,
        target.value / "jsdocgenwork",
        jsdocCommand.value
      )
    }

  )
}