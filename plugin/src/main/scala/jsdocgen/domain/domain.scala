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
  lineno: Int,
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
sealed trait Doclet {
  def name: String
  val longname: String
  def meta: Meta
}

trait HasParent {
  def memberof : String
}

trait HasAccess {
  def access : String
}

trait PackageMember extends HasParent {
  def longname: String

  def splitName = longname.split('.')
}


trait TypedefLike extends Doclet


trait HasType {
  def type_ : Type
  def `type`: Type = Option(type_).getOrElse(UnknownType)
}

case class Param(
  @JsonProperty("type") type_ : Type = UnknownType,
  @JsonProperty("name") name_ : String
) extends HasType {

  def name : String = Option(name_).getOrElse("unnamed_")

  def isOptional : Boolean = name.startsWith("opt_")

}

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
  inherited: Boolean,
  @JsonProperty("access") access_ : String = "public"
) extends Doclet with HasParent with HasType with HasAccess with TypedefLike {

  def access = if (name.contains('.')) "private" else access_

}

@key("namespace") case class Namespace(
  name: String,
  longname: String,
  meta: Meta,
  memberof: String = null
) extends Doclet

trait DefinedType {
  val longname: String
}

trait HasParams {
  def params_ : Seq[Param]
  def params : Seq[Param] = Option(params_).getOrElse(Seq()).filter(!_.name.contains('.'))
}

trait HasReturns {
  def returns_ : Seq[Return]
  def returns = Option(returns_).getOrElse(Seq()).headOption
}

@key("function") case class Function(
  name: String,
  memberof: String = null,
  scope: String,
  meta: Meta,
  longname: String,
  undocumented: Boolean,
  inherited: Boolean,
  `override`: Boolean,
  @JsonProperty("overrides") overrides_ : String,
  @JsonProperty("params") params_ : Seq[Param] = Seq(),
  @JsonProperty("returns") returns_ : Seq[Return] = Seq()
) extends Doclet with PackageMember with HasParams with HasReturns {
  def overrides = Option(overrides_)
}

@key("class") case class Class(
  name: String,
  memberof: String = null,
  scope: String,
  longname: String,
  meta : Meta,
  access: String = "public",
  @JsonProperty("params") params_ : Seq[Param] = Seq(),
  @JsonProperty("augments") augments_ : Seq[String] = Seq()
) extends Doclet with PackageMember with DefinedType with HasParams {
  def augments = Option(augments_).getOrElse(Seq()).headOption
}


@key("typedef") case class Typedef(
  name: String,
  longname: String,
  meta: Meta,
  memberof: String = null,
  @JsonProperty("type") type_ : Type = UnknownType,
  @JsonProperty("params") params_ : Seq[Param] = Seq(),
  @JsonProperty("returns") returns_ : Seq[Return] = Seq()
) extends Doclet with HasParent with DefinedType with HasType with HasParams with HasReturns with TypedefLike

@key("event") case class Event(
  name:String,
  longname: String,
  meta: Meta
) extends Doclet

@key("constant") case class Constant(
  name:String,
  longname: String,
  meta: Meta
) extends Doclet

@key("interface") case class Interface(
  name:String,
  longname: String,
  memberof: String = null,
  scope: String,
  meta: Meta
) extends Doclet with PackageMember

@key("package") case class Package(
  name:String,
  longname: String,
  meta: Meta
) extends Doclet

@key("file") case class File(
  name:String,
  longname: String,
  meta: Meta
) extends Doclet

@key("module") case class Module(
  name:String,
  longname: String,
  meta: Meta
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
    new NamedType(classOf[Function], "function"),
    new NamedType(classOf[Module], "module")
  )

  def fromJson[T](json: String)(implicit m : Manifest[T]): T = {
    mapper.readValue[T](json)
  }
}
