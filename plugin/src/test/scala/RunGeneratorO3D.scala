import java.io.File

import jsdocgen.domain.Doclet
import jsdocgen.domain.pickle._
import jsdocgen.generator.Generator

import scala.io.Source

/**
  * Created by marci on 08-11-2015.
  */
object RunGeneratorO3D extends App {

   val doclets = {
     val json = Source.fromURL(getClass.getResource("/o3d-jsdoc.json").toURI.toURL, "UTF-8").mkString
     read[Seq[Doclet]](json)
   }


   Generator.generate(
     new File("target/generatedo3d"),
     doclets
   )

 }
