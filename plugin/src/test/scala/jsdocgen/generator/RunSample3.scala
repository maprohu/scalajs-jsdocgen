package jsdocgen.generator

import java.io.File

/**
  * Created by pappmar on 19/01/2016.
  */
object RunSample3 extends App {

  val js =
    """
      |/**
      | * @namespace oli.control
      | */
      |
      |/**
      |* @interface
      |*/
      |oli.control.Control = function() {};
      |/**
      |* @param {ol.Map} map Map.
      |* @return {undefined} Undefined.
      |*/
      |oli.control.Control.prototype.setMap = function(map) {};
      |
    """.stripMargin

  val out = new File("target/runsample3")

  SampleRunner.run(js, out)

}
