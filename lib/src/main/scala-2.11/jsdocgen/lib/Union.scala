import scala.scalajs.js

package jsdocgen {

  package object lib {

    implicit def union2jsAny(value: Union) : js.Any = value.toJsAny

  }

  package lib {


    trait Union {
      def toJsAny : js.Any
    }
    case class UnionImpl[A](v: A)(implicit ev: A => js.Any) extends Union {
      override def toJsAny: js.Any = ev(v)
    }

    object undefined extends Union {
      override def toJsAny: js.Any = js.undefined
    }

  }

}



