package jsdocgen.generator

import java.io.{File, FileOutputStream, ByteArrayInputStream}
import java.net.URI

import sbt.{Path, IO}

/**
  * Created by pappmar on 19/01/2016.
  */
object RunSample extends App {

  val js =
    """
      |/**
      | * @namespace ol.layer
      | */
      |
      |/**
      |* @classdesc
      |* Vector data that is rendered client-side.
      |* Note that any property set in the options is set as a {@link ol.Object}
      |* property on the layer object; for example, setting `title: 'My Title'` in the
      |* options means that `title` is observable, and has get/set accessors.
      |*
      |* @constructor
      |* @extends {ol.layer.Layer}
      |* @fires ol.render.Event
      |* @param {olx.layer.VectorOptions=} opt_options Options.
      |* @api stable
      |*/
      |ol.layer.Vector = function(opt_options) {
      |}
      |
      |/**
      | * @namespace olx.layer
      | */
      |
      |/**
      |* @typedef {{renderOrder: (function(ol.Feature, ol.Feature):number|null|undefined),
      |* minResolution: (number|undefined),
      |* maxResolution: (number|undefined),
      |* opacity: (number|undefined),
      |* renderBuffer: (number|undefined),
      |* source: (ol.source.Vector|undefined),
      |* map: (ol.Map|undefined),
      |* style: (ol.style.Style|Array.<ol.style.Style>|ol.style.StyleFunction|undefined),
      |* updateWhileAnimating: (boolean|undefined),
      |* updateWhileInteracting: (boolean|undefined),
      |* visible: (boolean|undefined)}}
      |* @api
      |*/
      |olx.layer.VectorOptions;
    """.stripMargin

  val out = new File("target/runsample")

  SampleRunner.run(js, out)

}
