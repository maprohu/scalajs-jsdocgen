package jsdocgen.domain

import com.fasterxml.jackson.annotation.{JsonProperty, JsonTypeInfo}
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.databind.jsontype.NamedType
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import jsdocgen.domain.pickle.key
import upickle.Js

case class Meta(
  filename: String,
  path: String,
  code: Code
)


case class Code(
  value: Any = null
)

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "kind"
)
sealed trait Doclet

trait HasParent {
  def memberof : String
}

trait PackageMember extends HasParent {
  def name: String
  def meta: Meta
  def longname: String

  def splitName = longname.split('.')
}



trait HasType {
  def type_ : Type
  def `type`: Type = Option(type_).getOrElse(UnknownType)
}

case class Param(
  @JsonProperty("type") type_ : Type = UnknownType,
  name: String
) extends HasType

case class Return(
  @JsonProperty("type") type_ : Type = UnknownType
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
  @JsonProperty("type") type_ : Type = UnknownType,
  meta: Meta,
  scope: String,
  undocumented: Boolean,
  inherited: Boolean
) extends Doclet with HasParent with HasType

@key("namespace") case class Namespace(
  name: String,
  longname: String,
  memberof: String = null
) extends Doclet

trait DefinedType {
  val longname: String
}

trait HasParams {
  def params_ : Seq[Param]
  def params = Option(params_).getOrElse(Seq())
}

@key("function") case class Function(
  name: String,
  memberof: String = null,
  scope: String,
  meta: Meta,
  longname: String,
  @JsonProperty("params") params_ : Seq[Param] = Seq(),
  @JsonProperty("returns") returns_ : Seq[Return] = Seq()
) extends Doclet with PackageMember with HasParams {
  def returns = Option(returns_).getOrElse(Seq()).headOption
}

@key("class") case class Class(
  name: String,
  memberof: String = null,
  scope: String,
  longname: String,
  meta : Meta,
  @JsonProperty("params") params_ : Seq[Param] = Seq(),
  @JsonProperty("augments") augments_ : Seq[String] = Seq()
) extends Doclet with PackageMember with DefinedType with HasParams {
  def augments = Option(augments_).getOrElse(Seq()).headOption
}


@key("typedef") case class Typedef(
  name: String,
  longname: String,
  memberof: String = null,
  @JsonProperty("type") type_ : Type = UnknownType
) extends Doclet with HasParent with DefinedType with HasType

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


object JsonUtil {
  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  mapper.registerSubtypes(
    new NamedType(classOf[Member], "member"),
    new NamedType(classOf[Namespace], "namespace"),
    new NamedType(classOf[Class], "class"),
    new NamedType(classOf[Typedef], "typedef"),
    new NamedType(classOf[Event], "event"),
    new NamedType(classOf[Constant], "constant"),
    new NamedType(classOf[Interface], "interface"),
    new NamedType(classOf[Package], "package"),
    new NamedType(classOf[File], "file"),
    new NamedType(classOf[Function], "function")
  )

  def fromJson[T](json: String)(implicit m : Manifest[T]): T = {
    mapper.readValue[T](json)
  }
}
