/**
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */

package declarativewidgets.util

import declarativewidgets.Default
import org.apache.spark.sql.DataFrame
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, FunSpec}
import org.mockito.Mockito._
import play.api.libs.json._

class SerializationSupportSpec extends FunSpec with Matchers with MockitoSugar  {

  class TestSupport extends SerializationSupport {
    override def dataFrameWrites(limit: Int) = Writes {
      (df: DataFrame) => Json.obj()
    }
  }

  describe("SerializationSupport") {

    describe("#serialize") {
      it("should serialize as a DataFrame when the argument is a DataFrame") {
        val d: DataFrame = mock[DataFrame]

        val support = spy(new TestSupport)

        support.serialize(d)

        verify(support).dataFrameWrites(Default.Limit)
      }

      it("should serialize a float as a Number") {
        val d: Float = 3.0f

        val support = spy(new TestSupport)

        support.serialize(d) should be(JsNumber(3.0))
      }

      it("should serialize an Int as a Number") {
        val d: Int = 3

        val support = spy(new TestSupport)

        support.serialize(d) should be(JsNumber(3))
      }

      it("should serialize a sequence as a JsArray") {
        val d = Seq(3, "a", 4.1)
        val dNonObjElements = Seq(3, 3)

        val support = spy(new TestSupport)

        support.serialize(d) should be(
          JsArray(Seq(JsNumber(3), JsString("a"), JsNumber(4.1)))
        )
        support.serialize(dNonObjElements) should be(
          JsArray(Seq(JsNumber(3), JsNumber(3)))
        )
      }

      it("should serialize a array as a JsArray") {
        val d = Array(3, "a", 4.1)
        val dNonObjElements = Array(3, 3)

        val support = spy(new TestSupport)

        support.serialize(d) should be(
          JsArray(Seq(JsNumber(3), JsString("a"), JsNumber(4.1)))
        )
        support.serialize(dNonObjElements) should be(
          JsArray(Seq(JsNumber(3), JsNumber(3)))
        )
      }

      it("should serialize a list as a JsArray") {
        val d = List(3, "a", 4.1)
        val dNonObjElements = List(3, 3)

        val support = spy(new TestSupport)

        support.serialize(d) should be(
          JsArray(Seq(JsNumber(3), JsString("a"), JsNumber(4.1)))
        )
        support.serialize(dNonObjElements) should be(
          JsArray(Seq(JsNumber(3), JsNumber(3)))
        )
      }

      it("should serialize a map as a JsObject") {
        val d = Map("a" -> 2, "b" -> "c")

        val support = spy(new TestSupport)

        support.serialize(d) should be(
          JsObject(Seq(("a", JsNumber(2)), ("b", JsString("c"))))
        )
      }

      it("should serialize as a String by default") {
        val d = mock[Object]

        val support = spy(new TestSupport)

        support.serialize(d) should be(Json.toJson(d.toString))

      }

      it("should serialize true as a JSBoolean") {
        val d: Boolean = true

        val support = spy(new TestSupport)

        support.serialize(d) should be(JsBoolean(true))

      }

      it("should serialize false as a JSBoolean") {
        val d: Boolean = false

        val support = spy(new TestSupport)

        support.serialize(d) should be(JsBoolean(false))

      }
    }

    describe("toArray") {
      it("should return an array in the order specified by columns") {
        val support = new TestSupport()

        val rowJson = Json.parse(
          """
            {
              "name": "Joe",
              "age": 35,
              "last_name": "Smith",
              "b_day": "01-01-80"
            }
          """.stripMargin).as[JsObject]


        support.toArray(rowJson, Array("name", "last_name", "age", "b_day")) should be(
          Array(
            JsString("Joe"),
            JsString("Smith"),
            JsNumber(35),
            JsString("01-01-80")
          )
        )
      }
    }
  }
}