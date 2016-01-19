package jsdocgen.generator

import java.net.URI

import sbt._


/**
 * Created by pappmar on 12/11/2015.
 */
object JsdocInvoker {

  def run(
    source: URI,
    jsdocInputs : Seq[String] = Seq("."),
    out: File,
    work: File,
    jsdoc: Seq[String] = Seq("cmd", "/C", "jsdoc"),
    jsdocOptions : Seq[String] = Seq("--explain", "--recurse")
  ): File = {
    println("running jsdoc...")
    val s = RetrieveUnit(
      new BuildLoader.ResolveInfo(
        source,
        work,
        null,
        null
      )
    ).get

    out.getParentFile.mkdirs()
    val dir = s()

    println(s"jsdoc source base directory: ${dir}")
    (
      jsdoc ++
      jsdocOptions ++
      jsdocInputs.map { input =>
        val inputFile = new File(input)

        val f =
          if (inputFile.isAbsolute) input else
          new File(dir, input).getCanonicalPath

        println(s"jsdoc source: ${f}")

        f
      }
    ) #> out !

    println("jsdoc complete.")

    out
  }

}
