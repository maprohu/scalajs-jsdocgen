package jsdocgen.generator

import java.io.File

/**
  * Created by pappmar on 19/01/2016.
  */
object RunSample2 extends App {

  val js =
    """
      |/**
      | * @namespace ol.style
      | */
      |/**
      | * @namespace ol.structs
      | */
      |
      |
      |/**
      |* @interface
      |*/
      |ol.structs.IHasChecksum = function() {
      |};
      |/**
      |* @return {string} The checksum.
      |*/
      |ol.structs.IHasChecksum.prototype.getChecksum = function() {
      |};
      |
      |/**
      |* @classdesc
      |* Set stroke style for vector features.
      |* Note that the defaults given are the Canvas defaults, which will be used if
      |* option is not defined. The `get` functions return whatever was entered in
      |* the options; they will not return the default.
      |*
      |* @constructor
      |* @param {olx.style.StrokeOptions=} opt_options Options.
      |* @implements {ol.structs.IHasChecksum}
      |* @api
      |*/
      |ol.style.Stroke = function(opt_options) {
      |}
      |
      |/**
      |* @inheritDoc
      |*/
      |ol.style.Stroke.prototype.getChecksum = function() {
      |if (this.checksum_ === undefined) {
      |var raw = 's' +
      |(this.color_ ?
      |ol.color.asString(this.color_) : '-') + ',' +
      |(this.lineCap_ !== undefined ?
      |this.lineCap_.toString() : '-') + ',' +
      |(this.lineDash_ ?
      |this.lineDash_.toString() : '-') + ',' +
      |(this.lineJoin_ !== undefined ?
      |this.lineJoin_ : '-') + ',' +
      |(this.miterLimit_ !== undefined ?
      |this.miterLimit_.toString() : '-') + ',' +
      |(this.width_ !== undefined ?
      |this.width_.toString() : '-');
      |var md5 = new goog.crypt.Md5();
      |md5.update(raw);
      |this.checksum_ = goog.crypt.byteArrayToString(md5.digest());
      |}
      |return this.checksum_;
      |};
      |
    """.stripMargin

  val out = new File("target/runsample2")

  SampleRunner.run(js, out)

}
