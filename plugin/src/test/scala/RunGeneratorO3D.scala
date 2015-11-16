import java.io.File

import jsdocgen.domain.Doclet
import jsdocgen.generator.Generator

import scala.io.Source

/**
  * Created by marci on 08-11-2015.
  */
object RunGeneratorO3D extends App {
  val json = Source.fromURL(getClass.getResource("/o3d-jsdoc.json").toURI.toURL, "UTF-8").mkString

  Generator.generateFromString(
    new File("target/generatedo3d"),
    json
  )

}

