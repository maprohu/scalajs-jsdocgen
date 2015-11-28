package jsdocgen.generator

import java.io.{File, PrintWriter}


import jsdocgen.domain
import jsdocgen.domain._
import sbt.{URI, URL}
import scala.collection.breakOut

import scala.io.Source

/**
 * Created by marci on 08-11-2015.
 */
object Generator {
  def generateFromFile(
    targetDir: File,
    docletsFile: File,
    rootPackage : Seq[String] = Seq("jsfacade"),
    utilPackage : String = "pkg",
    implicits : Seq[String] = Seq("implicits")
  ) : Seq[File] = {
    println("reading json: " + docletsFile)

    generateFromString(
      targetDir,
      Source.fromFile(docletsFile, "UTF-8").mkString,
      rootPackage,
      utilPackage,
      implicits
    )
  }

  def readDoclets(json: String) : Seq[Doclet] = {
    JsonUtil.fromJson[Seq[Doclet]](json)
  }

  def generateFromString(
    targetDir: File,
    doclets: String,
    rootPackage : Seq[String] = Seq("jsfacade"),
    utilPackage : String = "pkg",
    implicits : Seq[String] = Seq("implicits")
  ) : Seq[File] = {

    generate(
      targetDir,
      readDoclets(doclets),
      rootPackage,
      utilPackage,
      implicits
    )
  }

  def generate(
    targetDir: File,
    doclets: Seq[Doclet],
    rootPackage : Seq[String] = Seq("jsfacade"),
    utilPackage : String = "pkg",
    implicits : Seq[String] = Seq("implicits")
  ) : Seq[File] = {
    new Generator(targetDir, doclets, rootPackage, utilPackage, implicits).generated
  }

}

/**
  * @see <a href="https://github.com/maprohu/scalajs-o3d/blob/master/facade/src/main/javascript/o3d-webgl/base.js#L50">source</a>
  *
  * @param targetDir
  * @param doclets
  * @param rootPackage
  * @param utilPackage
  * @param implicits
  */
class Generator (
  targetDir: File,
  doclets: Seq[Doclet],
  rootPackage : Seq[String] = Seq("jsfacade"),
  utilPackage : String = "pkg",
  implicits : Seq[String] = Seq("implicits"),
  sourceFileRoot : URI = new File("/home/marci/git/scalajs-o3d").toURI,
  sourcePubhlishRoot : URI = new URL("https://github.com/maprohu/scalajs-o3d/blob/master/").toURI
) {
  val libPackage = "jsdocgen.lib"
  val unionClassName = libPackage + ".Union"
  val unionImplClass = libPackage + ".UnionImpl"
  val undefinedObject = libPackage + ".undefined"

  val reserved = Set(
    "clone",
    "toString"
  )

  val keyword = Set(
    "type",
    "val",
    "object"
  )
  def isReserved(name: String) = reserved.contains(name)

  def id(from: String) =
    if (isReserved(from)) from + "_"
    else if (keyword.contains(from)) s"`$from`"
    else from

  val undefinedType = undefinedObject+".type"
  val jsAny = "scala.scalajs.js.Any"
  val jsObject = "scala.scalajs.js.Object"

  def linkSource(meta: Meta) : URI = {
    val uri = sourcePubhlishRoot.resolve(
      sourceFileRoot relativize new File(meta.path, meta.filename).toURI
    ).resolve(s"#L${meta.lineno}")

    uri
  }

  val unionClass = (rootPackage ++ implicits).mkString(".")

  trait ResolvedType {
    def toSet : Set[String]
    def join(other: ResolvedType) = ResolvedType(toSet ++ other.toSet, optional || other.optional)
    def optional : Boolean
    def toSingle : String
    def toJsType : String = if (optional) s"scala.scalajs.js.UndefOr[$toSingle]" else toSingle
    def toJsParamType = if (optional) s"$toJsType = scala.scalajs.js.undefined" else toJsType
  }
  case class SingleType(name: String, optional: Boolean = false) extends ResolvedType {
    val toSet = Set(name)
    def toSingle = name
    override def toString = name
  }
  case class UnionType(names: Set[String], optional: Boolean = false) extends ResolvedType {
    def toSet = names
    def toSingle = jsAny + s" /* $generatedName */"
    def generatedName = (if (optional) names + undefinedType else names).toSeq.sorted.mkString("`", "|", "`")
    override def toString = unionClass + "." + generatedName
  }
  object OptionalType extends ResolvedType {
    def toSet = Set()
    def toSingle = ??? // should not happen
    def optional = true
  }
  object UnionType {
    def apply(optional: Boolean, names: String*) : UnionType = UnionType(Set(names:_*), optional)
  }
  object ResolvedType {
    def apply(names: Set[String], optional: Boolean) = names.size match {
      case 0 => throw new RuntimeException
      case 1 => SingleType(names.iterator.next, optional)
      case _ => UnionType(names, optional)
    }
  }


  val realNamespaces: Seq[Seq[String]] = doclets
    .collect({
      case d: Namespace => d.longname.split('.').toSeq
    })
  val namespaces = realNamespaces :+ Seq()

  val functions = doclets
    .collect({
      case d : domain.Function => d
    })

  val classes = doclets
    .collect({
      case d : domain.Class => d
    })

  val typedefs = doclets
    .collect({
      case d : domain.Typedef => d
    })

  val members = doclets
    .collect({
      case d : domain.Member => d
    })


  val packageList = (o:String) =>
    Option(o).map(_.split('.').toSeq).getOrElse(Seq())

  val parentProp = (o:HasParent) =>
    packageList(o.memberof)

  val namespaceByParent = realNamespaces.groupBy(_.init).withDefaultValue(Seq())
  val functionByParent = functions.groupBy(parentProp).withDefaultValue(Seq())
  val classByParent = classes.groupBy(parentProp).withDefaultValue(Seq())
  val typedefByParent = typedefs.groupBy(parentProp).withDefaultValue(Seq())
  val memberByParent = members.groupBy(parentProp).withDefaultValue(Seq())

//    val googTypedefs = for {
//      ns <- namespaces
//      m <- memberByParent(ns)
//      if m.meta.code.value == "goog.typedef"
//    } yield m

  val typedefByName : Map[String, Typedef] = typedefs.map(td => td.longname -> td)(scala.collection.breakOut)

  val (realTypedefs, functionTypedefs) = typedefs.partition(_.`type`.names != Seq("function"))

  val unionTypedefs : Map[String, Type] =
    realTypedefs.collect({
      case td:Typedef if td.`type` != null && !td.`type`.names.isEmpty =>
        td.longname -> td.`type`
    })(breakOut)

  def definedTypeRef(longname: String) =
    (rootPackage ++ longname.split('.')).mkString(".")

  def functionTypeRef(longname: String) = {
    val names = longname.split('.')
    (rootPackage ++ names.init :+ utilPackage :+ names.last).mkString(".")
  }


  val definedTypesByName : Map[String, ResolvedType] =
    (
      (classes ++ realTypedefs).map(dt => dt.longname -> SingleType(definedTypeRef(dt.longname)))
      ++
      functionTypedefs.map(dt => dt.longname -> SingleType(functionTypeRef(dt.longname)))
    )(breakOut)





  val builtins : Map[String, ResolvedType] = Map(
    "string" -> SingleType("java.lang.String"),
    "number" -> UnionType(false, "scala.Byte", "scala.Short", "scala.Int", "scala.Float", "scala.Double"),
    "Element" -> SingleType("org.scalajs.dom.raw.Element"),
    "HTMLCanvas" -> SingleType("org.scalajs.dom.raw.HTMLCanvasElement"),
    "undefined" -> OptionalType
  )


  val arrayPattern1 = """Array\.<(.*)>""".r
  val arrayPattern2 = """(.*)\[\]""".r

  def resolveArray(name:String) : Option[ResolvedType] = {
    def arr(element: String) =
      Some(SingleType(s"scala.scalajs.js.Array[${resolveSingle(element)}]"))

    name match {
      case arrayPattern1(element) => arr(element)
      case arrayPattern2(element) => arr(element)
      case _ => None
    }
  }

  def resolveDefined(name: String) : Option[ResolvedType] =
    unionTypedefs.get(name).map(resolveUnion(_))
      .orElse(
        definedTypesByName.get(name)
      )



  def resolve(name: String) : ResolvedType =
    builtins.get(name)
      .orElse(
        resolveArray(name)
      )
      .orElse(
        resolveDefined(name)
      )
      .getOrElse(
        SingleType(jsAny)
      )

  def resolveUnion(t: domain.Type) : ResolvedType = {
    t.names.map(n => resolve(n)).reduce(_ join _)
  }


  def isOptional(name: domain.Type) : Boolean =
    resolveUnion(name).optional

  def resolveSingle(name: String) : String =
    resolve(name).toJsType

  def resolveSingleType(name: domain.Type) : String =
    resolveSingle(name.names.head)

  def resolveType(name: domain.Type, optional: Boolean = false) : ResolvedType = {
    val utypes = resolveUnion(name)
    if (optional) utypes join OptionalType else utypes
  }

  def resolveParam(param: domain.Param) : ResolvedType = {
    resolveType(
      param.`type`,
      param.isOptional
    )
  }

  def resolveReturn(ret: Option[domain.Return]) : ResolvedType =
    ret
      .map(name => resolveType(name.`type`))
      .getOrElse(SingleType("Unit"))



  def indent(str: String, level: Int) : String = {
    str.split('\n').map(("  " * level) + _).mkString("\n")
  }

  case class Out(out: PrintWriter, level: Int) {
    def write(str: String) = out.println(indent(str, level))
    def nest = copy(level = level+1)
  }


  def writeStatics(nsName: Seq[String], out: Out) : Unit = {
    import out.write

    for {
      fn <- functionByParent(nsName)
    } {
      if (isReserved(fn.name))
        write(s"""@scala.scalajs.js.annotation.JSName("${fn.name}")""")
      write(s"def ${id(fn.name)}(")

      write(
        (for { p <- fn.params } yield {

          val t = resolveParam(p)
          s"    ${id(p.name)} : ${t.toJsParamType}"

        }).mkString(",\n")
      )
      write(s") : ${resolveReturn(fn.returns).toJsType} = scala.scalajs.js.native")
      write("")
    }

    for {
      m <- memberByParent(nsName)
      if !m.inherited && !m.undocumented && m.scope == "static" && !m.name.endsWith("[undefined]")
    } {
      if (isReserved(m.name))
        write(s"""@scala.scalajs.js.annotation.JSName("${m.name}")""")
      write(s"""var ${id(m.name)} : ${resolveType(m.`type`).toJsType} = scala.scalajs.js.native""")
    }


    for {
      ns <- namespaceByParent(nsName)
    } {
      val m = ns.last
      if (isReserved(m))
        write(s"""@scala.scalajs.js.annotation.JSName("${m}")""")
      write(s"""  var ${id(m)} : ${packageJoin(ns :+ utilPackage)} = scala.scalajs.js.native""")

    }
  }

  def writeFunctionTypes(nsName: Seq[String], out: Out) : Unit = {
    import out.write

    for {
      m <- typedefByParent(nsName)
      if m.`type`.names == Seq("function")
    } {
      write(s"""type ${id(m.name)} = scala.scalajs.js.Function${m.params.size}[""")

      for {
        p <- m.params
      } {
        write(s"""  ${resolveParam(p).toJsType},""")
      }

      write(s"""  ${resolveReturn(m.returns).toJsType}""")

      write(s"""]""")
    }

  }

  def writeClass(cl: domain.Class, out: Out) = {
    import out.write

    write(s"// ${linkSource(cl.meta)}")
    write(s"@scala.scalajs.js.native")
    write( s"""@scala.scalajs.js.annotation.JSName("${cl.longname}")""")

    val superClass: String =
      cl.augments
        .flatMap(au => resolveDefined(au).map(_.toJsType))
        .headOption.getOrElse("scala.scalajs.js.Object")

    write(s"class ${cl.name} extends $superClass {")
    write("")
    writeClassMembers(cl, out.nest)
    write(s"}")
  }

  def writeClassMembers(cl: domain.Class, out: Out) = {
    import out.write

    if (!cl.params.isEmpty) {
      write("def this(")
      write(
        cl.params.map({ param =>
          s"  ${id(param.name)} : ${resolveParam(param).toJsParamType}"
        }).mkString(",\n")
      )
      write(") = this()")
      write("")
    }


    for {
      m <- memberByParent(cl.splitName)
      if m.access != "private" && !m.inherited && !m.undocumented && m.scope == "instance" && !m.name.endsWith("[undefined]")
    } {
      write(s"// ${linkSource(m.meta)}")
      if (isReserved(m.name))
        write(s"""@scala.scalajs.js.annotation.JSName("${m.name}")""")

      write(s"""var ${id(m.name)} : ${resolveType(m.`type`).toJsType} = scala.scalajs.js.native""")
      write("")
    }

    for {
      fn <- functionByParent(cl.splitName)
      if !fn.undocumented && !fn.inherited && !fn.`override` && fn.overrides.isEmpty && fn.scope != "inner"
    } {
      write(s"// ${linkSource(fn.meta)}")
      if (isReserved(fn.name))
        write(s"""@scala.scalajs.js.annotation.JSName("${fn.name}")""")

      val defStart = s"def ${id(fn.name)}("

      val defParams =
        (for { p <- fn.params } yield {
          s"\n  ${id(p.name)} : ${resolveParam(p).toJsParamType}"
        }).mkString(",")

      val defEnd = s") : ${resolveReturn(fn.returns).toJsType} = scala.scalajs.js.native"

      if (defParams.isEmpty) {
        write(defStart + defEnd)
      } else {
        write(defStart + defParams)
        write(defEnd)
      }

      write("")
    }

  }

  def writeTypedef(td: domain.Typedef, out: Out) = {
    import out.write

    write(s"@scala.scalajs.js.native")
    write(s"trait ${td.name} extends scala.scalajs.js.Object {")

    val mems = memberByParent(packageList(td.longname))

    for {
      m <- mems
    } {
      if (isReserved(m.name))
        write(s"""@scala.scalajs.js.annotation.JSName("${m.name}")""")

      write(s"  var ${id(m.name)} : ${resolveType(m.`type`).toJsType} = scala.scalajs.js.native")
    }

    write(s"}")

    write(s"object ${td.name} {")
    write(s"  def apply(")
    write(
      (for { m <- mems } yield {
        s"    ${id(m.name)} : ${resolveType(m.`type`).toJsParamType}"
      }).mkString(",\n")
    )
    write(s"  ) = scala.scalajs.js.Dynamic.literal(")
    write(
      (for { m <- mems } yield {
        s"""    "${m.name}" -> ${id(m.name)}"""
      }).mkString(",\n")
    )
    write(s"  ).asInstanceOf[${td.name}]")
    write(s"}")

  }

  def writeImplicits(out: Out): Unit = {
    import out.write

    val memberUnions = for {
      ns <- namespaces
      td <- typedefByParent(ns)
      m <- memberByParent(td.longname.split('.'))
      union = resolveUnion(m.`type`)
    } yield union

    val staticUnions = for {
      ns <- namespaces
      parent <- (classByParent(ns).map(_.splitName.toSeq) :+ ns)
      td <- functionByParent(parent)
      union <- td.params.map(resolveParam(_)) :+ resolveReturn(td.returns)
    } yield union

    val unionSet = memberUnions ++ staticUnions

    for {
      union : UnionType <- unionSet.collect({case t:UnionType => t}).toSet
    } {
      val name = union.generatedName

//      write("@scala.scalajs.js.annotation.RawJSType // Don't do this at home!")
      write(s"sealed trait $name extends $unionClassName {")

      for {
        t <- union.names
      } {
        write(s"  def `as $t` = this.asInstanceOf[${unionImplClass}[$t]].v")
      }

      write(s"}")

      for {
        t <- union.names
      } {
        write(s"implicit def `$t -> ${name.tail}(v: $t) = new ${unionImplClass}(v) with $name")
      }

    }


  }


  def packageJoin(pkg: Seq[String]) =
    (rootPackage ++ pkg).mkString(".")

  def sourceFile(longname: String) = {
    val path = rootPackage ++ longname.split('.')
    val file = new File(targetDir, path.mkString("/") + ".scala")
    file.getParentFile.mkdirs()
    file
  }

  def writeFile(longname: String)(writer: Out => Unit) = {
    val file = sourceFile(longname)
    val pw = new PrintWriter(file)
    writer(Out(pw, 0))
    pw.close()
    file
  }

  val classFiles = for {
    ns <- namespaces
    cl <- classByParent(ns)
  } yield writeFile(cl.longname) { out =>
      out.write(s"package ${packageJoin(ns)}")
      writeClass(cl, out)
  }

  val traitFiles = for {
    ns <- namespaces
    td <- typedefByParent(ns)
    if td.`type`.names != Seq("function")
  } yield writeFile(td.longname) { out =>
    out.write(s"package ${packageJoin(ns)}")
    writeTypedef(td, out)
  }

  val packageFiles = for {
    ns <- namespaces
  } yield writeFile((ns :+ utilPackage).mkString(".")) { out =>
    import out.write

    write(s"package ${packageJoin(ns)}")
    if (ns.isEmpty) {
      write("@scala.scalajs.js.native")
      write(s"object ${utilPackage} extends scala.scalajs.js.GlobalScope {")
      writeFunctionTypes(ns, out.nest)
    }

    else {
//        write(s"""@scala.scalajs.js.annotation.JSName("${ns.mkString(".")}")""")
      write(s"object ${utilPackage} extends {")
      writeFunctionTypes(ns, out.nest)
      write(s"}")
      write("@scala.scalajs.js.native")
      write(s"trait ${utilPackage} extends scala.scalajs.js.Object {")
    }

    writeStatics(ns, out.nest)

    write(s"}")
    write("")
  }

//    val globalFile = writeFile(globalObject) { out =>
//      import out.write
//
//      write(s"package ${packageJoin(Seq())}")
//      write("@scala.scalajs.js.native")
//      write(s"object $globalObject extends scala.scalajs.js.GlobalScope with ${utilPackage}")
//      write("")
//    }

  val implicitsFile = writeFile(implicits.mkString(".")) { out =>
    import out.write

    write(s"package ${packageJoin(implicits.init)}")
    write(s"package object ${implicits.last} {")

    writeImplicits(out.nest)

    write(s"}")
    write("")
  }

  val generated = classFiles ++ traitFiles ++ packageFiles :+ implicitsFile
//  val generated = classFiles ++ traitFiles ++ packageFiles


}

