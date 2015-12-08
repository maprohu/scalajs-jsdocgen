import java.io.File
import java.net.URL

import jsdocgen.domain.{JsonUtil, Doclet}
import jsdocgen.generator.Generator

import scala.io.Source

/**
  * Created by marci on 08-11-2015.
  */
object RunGeneratorCesium extends App {
  def json = Source.fromURL(getClass.getResource("/cesium-1.16-jsdoc.json").toURI.toURL, "UTF-8").mkString
  def doclets = JsonUtil.fromJson[Seq[Doclet]](json)

  Generator.generateFromString(
    new File("target/generated-cesium"),
    json,
    new File("/home/maprohu/git/scalajs-cesium/facade/target/jsdocgenwork/c6269b0263a4f834e250/cesium").toURI,
    new URL("https://github.com/AnalyticalGraphicsInc/cesium/blob/1.16/").toURI
  )

}

