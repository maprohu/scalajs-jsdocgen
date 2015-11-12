package jsdocgen.domain

import jsdocgen.domain.pickle.key

case class Meta(
  filename: String,
  path: String
)

sealed trait Doclet

trait HasParent {
  def memberof : String
}

trait PackageMember extends HasParent {
  def name: String
  def meta: Meta
  def longname: String
}



@key("function") case class Function(
  name: String,
  memberof: String = null,
  scope: String,
  meta: Meta,
  longname: String,
  params: Seq[Param] = Seq(),
  returns: Option[Return] = None
) extends Doclet with PackageMember

trait HasType {
  val `type`: Type
}

case class Param(
  `type`: Type,
  name: String
) extends HasType

case class Return(
  `type`: Type
) extends HasType

case class Type(
  names: Seq[String]
)

object UnknownType extends Type(
  names = Seq("unknown")
)

@key("member") case class Member(
  name: String,
  longname: String,
  memberof: String = null,
  `type`: Type = UnknownType
) extends Doclet with HasParent

@key("namespace") case class Namespace(
  name: String,
  longname: String,
  memberof: String = null
) extends Doclet

trait DefinedType {
  val longname: String
}

@key("class") case class Class(
  name: String,
  memberof: String = null,
  scope: String,
  longname: String,
  meta : Meta,
  params: Seq[Param] = Seq(),
  augments: Option[String] = None
) extends Doclet with PackageMember with DefinedType


@key("typedef") case class Typedef(
  name: String,
  longname: String,
  memberof: String = null,
  `type` : Type = null
) extends Doclet with HasParent with DefinedType

@key("event") case class Event(
) extends Doclet

@key("constant") case class Constant(
) extends Doclet

@key("interface") case class Interface(
) extends Doclet

@key("package") case class Package(
) extends Doclet

@key("file") case class File(
) extends Doclet

object pickle extends upickle.AttributeTagged {
  def tagName = "kind"
}
