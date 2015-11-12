package jsdocgen.generator

import sbt._
import Keys._

object JsdocPlugin extends AutoPlugin {

  override val projectSettings: Seq[Setting[_]] = baseNonameSettings

  object autoImport {
    lazy val jsdocGenerate = taskKey[Seq[File]]("jsdoc-generate")

    lazy val jsdocTarget = taskKey[File]("jsdoc-target")

    lazy val jsdocDocletsFile = settingKey[File]("jsdoc-docletsfile")

    lazy val jsdocImplicits = settingKey[Seq[String]]("jsdoc-implicits")

    lazy val jsdocGlobalScope = settingKey[Seq[String]]("jsdoc-globalscope")

    lazy val jsdocUtilScope = settingKey[String]("jsdoc-utilscope")

    
  }

  import autoImport._

  lazy val baseNonameSettings: Seq[sbt.Def.Setting[_]] = Seq(
    jsdocTarget := (sourceManaged in Compile).value / "jsdocgen",
    jsdocGlobalScope := Seq("jsdocgen"),
    jsdocUtilScope := "pkg",
    jsdocImplicits := Seq("implicits"),
    jsdocGenerate := {
      Generator.generateFile(
        jsdocTarget.value,
        jsdocDocletsFile.value,
        jsdocGlobalScope.value,
        jsdocUtilScope.value,
        jsdocImplicits.value
      )
    }
  )
}