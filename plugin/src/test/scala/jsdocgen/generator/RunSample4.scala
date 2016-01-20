package jsdocgen.generator

import java.io.File

/**
  * Created by pappmar on 19/01/2016.
  */
object RunSample4 extends App {

  val js =
    """
      |/**
      | * @namespace oli.control
      | */
      |
      |/**
      |* @param {number|string|undefined} map Map.
      |*/
      |oli.control.setMap = function(map) {};
      |
    """.stripMargin

  val out = new File("target/runsample4")

  SampleRunner.run(js, out)

}
