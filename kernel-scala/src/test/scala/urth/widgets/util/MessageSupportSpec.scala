package urth.widgets.util

import com.ibm.spark.comm.CommWriter
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, FunSpec}
import play.api.libs.json.{JsNumber, JsString, Json}

class MessageSupportSpec extends FunSpec with Matchers with MockitoSugar {

  class TestSupport extends MessageSupport

  describe("#sendState") {
    it("should send a properly formatted update state message") {
      val expected = Json.obj("method" -> "update", "state" -> Map("a" -> "b"))
      val comm = mock[CommWriter]
      val test = new TestSupport
      test.sendState(comm, "a", JsString("b"))
      verify(comm).writeMsg(expected)
    }
  }
}
