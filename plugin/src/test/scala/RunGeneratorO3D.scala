import java.io.File
import java.net.URL

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
    json,
    new File("/home/marci/git/scalajs-ord/facade/src/main/javascript").toURI,
    new URL("https://github.com/maprohu/scalajs-o3d/blob/master/facade/src/main/javascript/").toURI
  )

}

