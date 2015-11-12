import jsdocgen.generator.JsdocInvoker

import sbt._
/**
 * Created by pappmar on 12/11/2015.
 */
object RunInvoker extends App {

  JsdocInvoker.run(
    uri("https://github.com/openlayers/ol3.git#v3.10.1"),
    Seq("src", "externs"),
    new File("target/jsdoc.json"),
    new File("target/jsdocwork")
  )



}
