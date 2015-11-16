import upickle.Js

object JsonSandbox extends  App {
  import domain._

  val json =
    """
      |[
      |  {"mt": "boo"},
      |  {"mt": 18}
      |]
    """.stripMargin


  val scala = pickle.read(json)

  println(scala)

}

package domain {
  object pickle {
    implicit val myTypeReader = upickle.default.Reader[MyType] {
      case v : Js.Value => new MyType(v)
    }

    def read(json: String) = upickle.default.read[Seq[Boo]](json)
  }
  class MyType(val v : Js.Value)

  case class Boo(mt: MyType)
}