import java.io.{File, PrintWriter}
import java.net.URL

import jsdocgen._
import jsdocgen.domain.pickle._
import jsdocgen.domain.{Doclet, Function}
import jsdocgen.generator.Generator

import scala.io.Source

/**
  * Created by marci on 08-11-2015.
  */
object RunGenerator extends App {


   Generator.generateFromString(
     new File("target/generated"),
     Source.fromURL(getClass.getResource("/ol3-3.10.1-jsdoc.json").toURI.toURL, "UTF-8").mkString,
     new File("D:\\git\\ol3").toURI,
     new URL("https://github.com/openlayers/ol3/blob/master/").toURI
   )

 }
