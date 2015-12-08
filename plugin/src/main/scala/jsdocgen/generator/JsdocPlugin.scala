package jsdocgen.generator

import java.util.Properties

import org.scalajs.sbtplugin.ScalaJSPlugin
import sbt._
import Keys._

object JsdocPlugin extends AutoPlugin {


  object autoImport {
    lazy val jsdocGenerate = taskKey[Seq[File]]("jsdoc-generate")

    lazy val jsdocTarget = taskKey[File]("jsdoc-target")

    lazy val jsdocWork = taskKey[File]("jsdoc-work")

    lazy val jsdocDocletsFile = taskKey[File]("jsdoc-docletsfile")

    lazy val jsdocRunTarget = settingKey[File]("jsdoc-runtarget")

    lazy val jsdocRunSource = settingKey[Option[URI]]("jsdoc-runsource")

    lazy val jsdocRunInputs = settingKey[Seq[String]]("jsdoc-runinputs")

    lazy val jsdocImplicits = settingKey[Seq[String]]("jsdoc-implicits")

    lazy val jsdocGlobalScope = settingKey[Seq[String]]("jsdoc-globalscope")

    lazy val jsdocUtilScope = settingKey[String]("jsdoc-utilscope")

    lazy val jsdocCommand = settingKey[Seq[String]]("jsdoc-command")

    lazy val jsdocRun = taskKey[File]("jsdoc-run")

    lazy val jsdocSourceFileRoot = taskKey[URI]("jsdoc-source-file-root")

    lazy val jsdocSourcePublishRoot = taskKey[URI]("jsdoc-source-publish-root")

  }

  import autoImport._

  lazy val props = {
    val p = new Properties
    p.load(getClass.getResourceAsStream("/jsdocgen/plugin.properties"))
    p
  }
  lazy val pluginVersion = props.getProperty("version")
  lazy val pluginOrganization = props.getProperty("organization")
  lazy val pluginLibName = props.getProperty("lib.name")

  import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

  override def requires: Plugins = ScalaJSPlugin

  override val projectSettings: Seq[Setting[_]] = Seq(
    libraryDependencies += pluginOrganization %%% pluginLibName % pluginVersion,
    jsdocCommand := {
      if (sys.props.get("os.name").exists(_.toLowerCase().contains("windows")))
        Seq("cmd", "/C", "jsdoc")
      else
        Seq("jsdoc")
    },
    jsdocTarget := (sourceManaged in Compile).value / "jsdocgen",
    jsdocGlobalScope := Seq("jsfacade"),
    jsdocUtilScope := "pkg",
    jsdocImplicits := Seq("implicits"),
    jsdocRunTarget := target.value / "jsdoc.json",
    jsdocDocletsFile := jsdocRun.value,
    jsdocRunSource := None,
    jsdocSourceFileRoot := jsdocRunSource.value.getOrElse(uri(".")),
    jsdocSourcePublishRoot := uri(s"https://github.com/maprohu/${name.value}/blob/master/facade/src/main/javascript/"),
    jsdocRunInputs := Seq("."),
    jsdocWork := target.value / "jsdocgenwork",
    jsdocGenerate := {
      Generator.generateFromFile(
        jsdocTarget.value,
        jsdocDocletsFile.value,
        jsdocSourceFileRoot.value,
        jsdocSourcePublishRoot.value,
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
        jsdocWork.value,
        jsdocCommand.value
      )
    }

  )
}