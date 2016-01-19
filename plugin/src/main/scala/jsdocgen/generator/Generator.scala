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
    sourceFileRoot : URI,
    sourcePubhlishRoot : URI,
    rootPackage : Seq[String] = Seq("jsfacade"),
    utilPackage : String = "pkg",
    implicits : Seq[String] = Seq("implicits")
  ) : Seq[File] = {
    println("reading json: " + docletsFile)

    generateFromString(
      targetDir,
      Source.fromFile(docletsFile, "UTF-8").mkString,
      sourceFileRoot,
      sourcePubhlishRoot,
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
    sourceFileRoot : URI,
    sourcePubhlishRoot : URI,
    rootPackage : Seq[String] = Seq("jsfacade"),
    utilPackage : String = "pkg",
    implicits : Seq[String] = Seq("implicits")
  ) : Seq[File] = {

    generate(
      targetDir,
      readDoclets(doclets),
      sourceFileRoot,
      sourcePubhlishRoot,
      rootPackage,
      utilPackage,
      implicits
    )
  }

  def generate(
    targetDir: File,
    doclets: Seq[Doclet],
    sourceFileRoot : URI,
    sourcePubhlishRoot : URI,
    rootPackage : Seq[String] = Seq("jsfacade"),
    utilPackage : String = "pkg",
    implicits : Seq[String] = Seq("implicits")
  ) : Seq[File] = {
    new Generator(targetDir, doclets, sourceFileRoot, sourcePubhlishRoot, rootPackage, utilPackage, implicits).generated
  }

}

/**
  * @see <a href="https://github.com/maprohu/scalajs-o3d/blob/master/facade/src/main/javascript/o3d-webgl/base.js#L50">source</a>
  * @param targetDir
  * @param doclets
  * @param rootPackage
  * @param utilPackage
  * @param implicits
  */
class Generator (
  targetDir: File,
  doclets: Seq[Doclet],
  sourceFileRoot : URI,
  sourcePubhlishRoot : URI,
  rootPackage : Seq[String] = Seq("jsfacade"),
  utilPackage : String = "pkg",
  implicits : Seq[String] = Seq("implicits")
) {


  val libPackage = "jsdocgen.lib"
  val unionClassName = libPackage + ".Union"
  val unionImplClass = libPackage + ".UnionImpl"
//  val undefinedObject = libPackage + ".undefined"

  val reserved = Set(
    "clone",
    "toString"
  )

  val keyword = Set(
    "type",
    "val",
    "object",
    "match"
  )
  def isReserved(name: String) = reserved.contains(name)

  def id(from: String) =
    if (isReserved(from)) from + "_"
    else if (keyword.contains(from)) s"`$from`"
    else from

//  val undefinedType = undefinedObject+".type"
  val jsAny = "scala.scalajs.js.Any"
  val jsObject = "scala.scalajs.js.Object"

  def linkSource(meta: Meta) : URI = {
    val uri = sourcePubhlishRoot.resolve(
      sourceFileRoot relativize new File(meta.path, meta.filename).toURI
    ).resolve(s"#L${meta.lineno}")

    uri
  }

  def log(msg: String) = println(msg)
  def log(msg: String, hasMeta: Doclet) = println(s"$msg - ${linkSource(hasMeta.meta)}")

  val unionClass = (rootPackage ++ implicits).mkString(".")

  trait ResolvedType {
    def toSet : Set[String]
    def join(other: ResolvedType) : ResolvedType = ResolvedType(toSet ++ other.toSet, optional || other.optional)
    def optional : Boolean
    def toSingle : String
    def toJsType : String = if (optional) s"scala.scalajs.js.UndefOr[$toSingle]" else toSingle
    def toJsParamType = if (optional) s"$toJsType = scala.scalajs.js.undefined" else toJsType

    def toWrapperType : String = if (optional) s"scala.scalajs.js.UndefOr[$toWrapperSingle]" else toWrapperSingle
    def toWrapperSingle : String /* = {
      val set = (if (optional) toSet + undefinedType else toSet)
      if (set.size == 1) set.head
      else unionClass + "." + set.toSeq.sorted.mkString("`", "|", "`")
    }*/
    def toWrapperParamType = if (optional) s"$toWrapperType = scala.scalajs.js.undefined" else toWrapperType
//    def toWrapperParamType = if (optional) s"$toWrapperType = $undefinedObject" else toWrapperType
  }
  case class SingleType(name: String, optional: Boolean = false) extends ResolvedType {
    val toSet = Set(name)
    def toSingle = name
    override def toString = name
    override def toWrapperSingle = name
  }
  case class UnionType(names: Set[String], optional: Boolean = false) extends ResolvedType {
    def toSet = names
    def toSingle = jsAny + s" /* $generatedName */"
    def generatedName = names.toSeq.sorted.mkString("`", "|", "`")
//    def generatedName = (if (optional) names + undefinedType else names).toSeq.sorted.mkString("`", "|", "`")
//    override def toString = toImplicitRef
//    def toImplicitRef = unionClass + "." + generatedName
    def toWrapperSingle = unionClass + "." + generatedName
  }
  object OptionalType extends ResolvedType {
    def toSet = Set()
    def toSingle = "Unit"
    override def toJsType : String = "Unit"

    override def join(other: ResolvedType) = other match {
      case UnkownType(_) => UnkownType(true)
      case OptionalType => OptionalType
      case _ => super.join(other)
    }

    // should not happen
    def optional = true
//    def toWrapperType = ??? // should not happen
    def toWrapperSingle = "scala.scalajs.js.Any" // TODO check this
  }
  case class UnkownType(optional: Boolean) extends ResolvedType {
    def toSet = Set(jsAny)
    def toSingle = jsAny
    override def toWrapperType = toJsType
    override def toWrapperParamType = toJsParamType
    override def join(other: ResolvedType) = other match {
      case UnkownType(otheropt) => UnkownType(optional || otheropt)
      case OptionalType => UnkownType(true)
      case _ => super.join(other)
    }
    def toWrapperSingle = toSingle
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
      case d : domain.Function if !d.ignore => d
    })

  val interfaces = doclets
    .collect({
      case d : domain.Interface => d
    })

  val classes = doclets
    .collect({
      case d : domain.Class => d
    })

  val typeAliases = doclets
    .collect({
      case d : domain.Member if d.meta.code.value == "goog.typedef" => d
    })

  val typedefs = doclets
    .collect({
      case d : domain.Typedef => d
      case d : domain.Member if d.meta.code.value == "goog.typedef" => d
    })

//  val typedefMemebers = doclets
//    .collect({
//    })

  val members = doclets
    .collect({
      case d : domain.Member if !d.ignore => d
    })


  val packageList = (o:String) =>
    Option(o).map(_.split('.').toSeq).getOrElse(Seq())

  val parentProp = (o:HasParent) =>
    packageList(o.memberof)

  val namespaceByParent = realNamespaces.groupBy(_.init).withDefaultValue(Seq())
  val functionByParent = functions.groupBy(parentProp).withDefaultValue(Seq())
  val classByParent = classes.groupBy(parentProp).withDefaultValue(Seq())
  val interfacesByParent = interfaces.groupBy(parentProp).withDefaultValue(Seq())
  val typedefByParent = typedefs.groupBy(parentProp).withDefaultValue(Seq())
  val memberByParent = members.groupBy(parentProp).withDefaultValue(Seq())

//    val googTypedefs = for {
//      ns <- namespaces
//      m <- memberByParent(ns)
//      if m.meta.code.value == "goog.typedef"
//    } yield m

  val typedefByName : Map[String, TypedefLike] = typedefs.map(td => td.longname -> td)(scala.collection.breakOut)
  val typeAliasByName : Map[String, Member] = typeAliases.map(td => td.longname -> td)(scala.collection.breakOut)

  val (realTypedefs, functionTypedefs) = typedefs.partition(_.`type`.names != Seq("function"))

  val unionTypedefs : Map[String, Type] =
    realTypedefs.collect({
      case td:Typedef if td.`type` != null && !td.`type`.names.isEmpty && td.`type`.names != Seq("Object") =>
        td.longname -> td.`type`
    })(breakOut)

  def definedTypeRef(longname: String) =
    (rootPackage ++ longname.split('.')).mkString(".")

  def functionTypeRef(longname: String) = {
    val names = longname.split('.')
    (rootPackage ++ names.init :+ utilPackage :+ names.last).mkString(".")
  }


  val definedTypesByNameMap : Map[String, ResolvedType] =
    (
      (classes ++ interfaces ++ realTypedefs).map(dt => dt.longname -> SingleType(definedTypeRef(dt.longname)))
      ++
      functionTypedefs.map(dt => dt.longname -> SingleType(functionTypeRef(dt.longname)))
    )(breakOut)



  val definedTypesByName : String => Option[ResolvedType] = { name =>
    typeAliasByName
      .get(name)
      .map(ta => resolveUnion(ta.`type`))
      .orElse(definedTypesByNameMap.get(name))
  }





  val builtins : Map[String, ResolvedType] = Map(
    "string" -> SingleType("java.lang.String"),
//    "number" -> UnionType(false, "scala.Byte", "scala.Short", "scala.Int", "scala.Float", "scala.Double"),
    "number" -> SingleType("scala.Double"),
    "Element" -> SingleType("org.scalajs.dom.raw.Element"),
    "HTMLCanvas" -> SingleType("org.scalajs.dom.raw.HTMLCanvasElement"),
    "undefined" -> OptionalType
  )


  val arrayPattern1 = """Array\.<!?(.*)>""".r
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
        definedTypesByName(name)
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
        UnkownType(false)
      )

  val typeNamePattern = """!?(.*)""".r

  def resolveUnion(t: domain.Type) : ResolvedType = {
    t.names.map({
      case typeNamePattern(n) => n
      case n => n
    }).map(
      resolve(_)
    ).reduce(_ join _)
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
      write(s"// ${linkSource(fn.meta)}")
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

    val subpackages = namespaceByParent(nsName)
    val subpackageNames = subpackages.map(_.last).toSet

    for {
      m <- memberByParent(nsName)
      if !m.inherited &&
        !m.undocumented &&
        m.scope == "static" &&
        !m.name.endsWith("[undefined]") &&
        !subpackageNames.contains(m.name)
    } {
      write(s"// ${linkSource(m.meta)}")
      if (isReserved(m.name))
        write(s"""@scala.scalajs.js.annotation.JSName("${m.name}")""")
      write(s"""val ${id(m.name)} : ${resolveType(m.`type`).toJsType} = scala.scalajs.js.native""")
    }

    write("// subpackages")
    for {
      ns <- subpackages
    } {
      val m = ns.last
      if (isReserved(m))
        write(s"""@scala.scalajs.js.annotation.JSName("${m}")""")
      write(s"""val ${id(m)} : ${packageJoin(ns :+ utilPackage)} = scala.scalajs.js.native""")
    }
    for {
      ns <- classByParent(nsName)
    } {
      write(s"// ${linkSource(ns.meta)}")
      val m = ns.name
      if (isReserved(m))
        write(s"""@scala.scalajs.js.annotation.JSName("${m}")""")
      write(s"""val ${id(m)} : ${packageJoin((nsName :+ m) :+ "statics")} = scala.scalajs.js.native""")
    }
  }

  def writeStaticsWrapper(nsName: Seq[String], out: Out) : Unit = {
    import out.write

    for {
      fn <- functionByParent(nsName)
    } {
      log(s"    writing staticsWrapper: ${fn}", fn)

      write(s"// ${linkSource(fn.meta)}")
      if (isReserved(fn.name))
        write(s"""@scala.scalajs.js.annotation.JSName("${fn.name}")""")
      write(s"def ${id(fn.name)}(")

      write(
        (for { p <- fn.params } yield {

          val t = resolveParam(p)
          s"  ${id(p.name)} : ${t.toWrapperParamType}"

        }).mkString(",\n")
      )

      write(s") : ${resolveReturn(fn.returns).toWrapperType} = _wrapped_.${id(fn.name)}(")

      write(
        (for { p <- fn.params } yield {
          val t = resolveParam(p)
          s"  ${id(p.name)}.asInstanceOf[${t.toJsType}]"
        }).mkString(",\n")
      )


      write(s").asInstanceOf[${resolveReturn(fn.returns).toWrapperType}]")
      write("")
    }

  }

  def writeFunctionTypes(nsName: Seq[String], out: Out) : Unit = {
    import out.write

    for {
      m <- typedefByParent(nsName).collect {case b:Typedef => b}
      if m.`type`.names == Seq("function")
    } {
      write(s"// ${linkSource(m.meta)}")

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

    val imps = cl.implements.flatMap(au => resolveDefined(au).map(_.toJsType).toSeq)

    write(s"class ${cl.name} extends ${(superClass +: imps).mkString(" with ")} {")
    write("")
    writeClassMembers(cl, out.nest)
    write(s"}")

    write(s"object ${cl.name} {")
    writeClassStaticMembers(cl, out.nest)
    write(s"}")
  }

  def writeInterface(cl: domain.Interface, out: Out) = {
    import out.write

    write(s"// ${linkSource(cl.meta)}")
    write(s"@scala.scalajs.js.native")
    write( s"""@scala.scalajs.js.annotation.JSName("${cl.longname}")""")

    val superClass: String =
        "scala.scalajs.js.Object"

//    write(s"trait ${cl.name} {")
    write(s"trait ${cl.name} extends $superClass {")
    write("")
    writeInterfaceMembers(cl, out.nest)
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

      for {
        fn <- functionByParent(cl.splitName)
        if !fn.undocumented && !fn.inherited && !fn.`override` && fn.overrides.isEmpty && fn.scope == "instance"
      } {
        write(s"// ${linkSource(fn.meta)}")
        if (isReserved(fn.name))
          write(s"""@scala.scalajs.js.annotation.JSName("${fn.name}")""")

        val defStart0 = s"def ${id(fn.name)}("

        val defStart = if (fn.implements.isEmpty) defStart0 else s"override $defStart0"

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
  }

  def writeInterfaceMembers(cl: domain.Interface, out: Out) = {
    import out.write

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
//      val defEnd = s") : ${resolveReturn(fn.returns).toJsType}"

      if (defParams.isEmpty) {
        write(defStart + defEnd)
      } else {
        write(defStart + defParams)
        write(defEnd)
      }

      write("")
    }

  }

  def writeClassStaticMembers(cl: domain.Class, out: Out) = {
    import out.write

    for {
      m <- typedefByParent(cl.splitName)
    } {
      write(s"type ${id(m.name)} = ${resolveUnion(m.`type`).toJsType}")
    }

    for {
      m <- classByParent(cl.splitName) ++ interfacesByParent(cl.splitName)
      if m.access != "private"
    } {
      write(s"// ${linkSource(m.meta)}")
      write(s"@scala.scalajs.js.native")
      write(s"trait ${id(m.name)} extends scala.scalajs.js.Object")
    }

    write(s"@scala.scalajs.js.native")
    write(s"trait statics extends scala.scalajs.js.Object {")

    val nest = out.nest
    for {
      m <- memberByParent(cl.splitName)
      if m.access != "private" && m.scope == "static" && !m.name.endsWith("[undefined]") && !m.undocumented
    } {
      nest.write(s"// ${linkSource(m.meta)}")
      if (isReserved(m.name))
        nest.write(s"""@scala.scalajs.js.annotation.JSName("${m.name}")""")

      nest.write(s"""var ${id(m.name)} : ${resolveType(m.`type`).toJsType} = scala.scalajs.js.native""")
      nest.write("")
    }

    write(s"}")

  }

  def writeTypedef(td: domain.TypedefLike, out: Out) = {
    import out.write

    write(s"// ${linkSource(td.meta)}")
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
      union : UnionType <- unionSet.collect({case t:UnionType => t.copy(optional = false)}).toSet
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
    println(s"  writing file: ${file}")
    val pw = new PrintWriter(file)
    writer(Out(pw, 0))
    pw.close()
    file
  }

  log("classFiles")
  val classFiles = for {
    ns <- namespaces
    cl <- classByParent(ns)
  } yield writeFile(cl.longname) { out =>
    out.write(s"package ${packageJoin(ns)}")
    writeClass(cl, out)
  }

  log("interfaceFiles")
  val interfaceFiles = for {
    ns <- namespaces
    cl <- interfacesByParent(ns)
  } yield writeFile(cl.longname) { out =>
      out.write(s"package ${packageJoin(ns)}")
      writeInterface(cl, out)
  }


  log("traitFiles")
  val traitFiles = for {
    ns <- namespaces
    td <- typedefByParent(ns).collect {case x:Typedef => x}
    if td.`type`.names == Seq("Object")
//    if td.`type`.names != Seq("function")
  } yield writeFile(td.longname) { out =>
    out.write(s"package ${packageJoin(ns)}")
    writeTypedef(td, out)
  }

  log("packageFiles")
  val packageFiles = for {
    ns <- namespaces
  } yield writeFile((ns :+ utilPackage).mkString(".")) { out =>
    {
      import out.write

      if (!ns.isEmpty) write(s"package ${packageJoin(ns.init)}")

      write(s"package object ${(rootPackage ++ ns).last} {")

      val nest = out.nest

      for {
        m <- typedefByParent(ns)
        if m.`type`.names != Seq("Object")
      } {
        nest.write(s"// ${linkSource(m.meta)}")
        nest.write(s"type ${id(m.name)} = ${resolveUnion(m.`type`).toJsType}")
      }

//      nest.write(s"object wrap {")
//      val nest2 = out.nest
//
//      for {
//        m <- typedefByParent(ns)
//      } {
//        nest2.write(s"// ${linkSource(m.meta)}")
//        nest2.write(s"type ${id(m.name)} = ${resolveUnion(m.`type`).toWrapperType}")
//      }
//
//      nest.write(s"}")

      write(s"}")
    }

    out.write(s"package ${(rootPackage ++ ns).last} {")

    {
      val nest = out.nest
      import nest.write

      if (ns.isEmpty) {
        write(s"@scala.scalajs.js.native")
        write(s"object global extends $utilPackage with scala.scalajs.js.GlobalScope {")
        write(s"}")
        write(s"")
      }

      write("@scala.scalajs.js.native")
      write(s"trait ${utilPackage} extends scala.scalajs.js.Object {")

      writeStatics(ns, nest.nest)

      write(s"}")

      write("")

      write(s"object $utilPackage {")

      writeFunctionTypes(ns, out.nest)

      write(s"  implicit class Wrapping(_wrapped_ : $utilPackage) {")
      write(s"    def wrap = new Wrapper(_wrapped_)")
      write(s"  }")

      write(s"  class Wrapper(_wrapped_ : $utilPackage) {")

      writeStaticsWrapper(ns, out.nest.nest)

      write(s"  }")

      write(s"}")
    }

    out.write(s"}")



  }

//    val globalFile = writeFile(globalObject) { out =>
//      import out.write
//
//      write(s"package ${packageJoin(Seq())}")
//      write("@scala.scalajs.js.native")
//      write(s"object $globalObject extends scala.scalajs.js.GlobalScope with ${utilPackage}")
//      write("")
//    }

  log("implicitsFile")
  val implicitsFile = writeFile(implicits.mkString(".")) { out =>
    import out.write

    write(s"package ${packageJoin(implicits.init)}")
    write(s"package object ${implicits.last} {")

    writeImplicits(out.nest)

    write(s"}")
    write("")
  }

  val generated = classFiles ++ interfaceFiles ++ traitFiles ++ packageFiles :+ implicitsFile
//  val generated = classFiles ++ traitFiles ++ packageFiles


}

