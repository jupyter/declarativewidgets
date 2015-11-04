package urth.widgets.util

import com.ibm.spark.comm.CommWriter
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, FunSpec}
import play.api.libs.json.{JsNumber, JsString, Json}
import urth.widgets.Comm

class MessageSupportSpec extends FunSpec with Matchers with MockitoSugar {

  class TestSupport extends MessageSupport {
    override def timestamp = 0
  }

  describe("#sendState") {
    it("should send a properly formatted update state message") {
      val expected = Json.obj("method" -> "update", "state" -> Map("a" -> "b"))
      val comm = mock[CommWriter]
      val test = new TestSupport
      test.sendState(comm, "a", JsString("b"))
      verify(comm).writeMsg(expected)
    }
  }

  describe("#sendStatus") {
    it("should send a properly formatted status message") {
      val expectedData = Json.obj(
        Comm.KeyStatus -> "status",
        Comm.KeyMessage -> "message",
        Comm.KeyTimestamp -> 0
      )
      val comm = mock[CommWriter]
      val test = spy(new TestSupport)
      test.sendStatus(comm, "status", "message")

      verify(test).sendState(comm, Comm.KeyStatusMsg, expectedData);
    }
  }
}
