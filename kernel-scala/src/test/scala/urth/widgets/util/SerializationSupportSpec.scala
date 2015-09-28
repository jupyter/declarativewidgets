/**
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */

package urth.widgets.util

import org.apache.spark.sql.DataFrame
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, FunSpec}
import org.mockito.Mockito._
import play.api.libs.json.{Json, JsString, Writes}
import urth.widgets.Default

class SerializationSupportSpec extends FunSpec with Matchers with MockitoSugar  {

  class TestSupport extends SerializationSupport {
    override def dataFrameWrites(limit: Int) = Writes {
      (df: DataFrame) => Json.obj()
    }
  }

  describe("SerializationSupport"){

    describe("#serialize") {
      it("should serialize as a DataFrame when the argument is a DataFrame") {
        val d: DataFrame = mock[DataFrame]

        val support = spy(new TestSupport)

        support.serialize(d)

        verify(support).dataFrameWrites(Default.Limit)
      }

      it("should serialize as a String by default") {
        val d = mock[Object]

        val support = spy(new TestSupport)

        support.serialize(d) should be (Json.toJson(d.toString))

      }
    }

  }
}