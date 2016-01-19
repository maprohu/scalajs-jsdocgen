package jsdocgen.generator

import java.io.{ByteArrayInputStream, File, FileOutputStream}
import java.net.URI

import sbt.{IO, Path}

/**
  * Created by pappmar on 19/01/2016.
  */
object SampleRunner {

  def run(js: String, out: File) = {
    import Path._

    val jsdocFile = out / "jsdoc.json"

    IO.withTemporaryDirectory { jsDir =>
      val jsFile = jsDir / "sample.js"
      IO.transferAndClose(new ByteArrayInputStream(js.getBytes), new FileOutputStream(jsFile))

      IO.withTemporaryDirectory { workDir =>
        JsdocInvoker.run(
          source = jsDir.toURI,
          jsdocInputs = Seq(jsFile.getCanonicalPath),
          out = jsdocFile,
          work = workDir
        )

        Generator.generateFromFile(
          out / "scala",
          jsdocFile,
          new URI("."),
          new URI(".")
        )
      }
    }
  }

}
