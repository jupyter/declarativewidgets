package declarativewidgets.util

import declarativewidgets.Comm
import org.apache.toree.comm.CommWriter
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, FunSpec}
import play.api.libs.json.{JsNumber, JsString, Json}

class MessageSupportSpec extends FunSpec with Matchers with MockitoSugar {

  class TestSupport(comm: CommWriter = mock[CommWriter]) extends MessageSupport(comm) {
    override def timestamp = 0
  }

  describe("#sendState") {
    it("should send a properly formatted update state message") {
      val expected = Json.obj("method" -> "update", "state" -> Map("a" -> "b"))
      val comm = mock[CommWriter]
      val test = new TestSupport(comm)
      test.sendState("a", JsString("b"))
      verify(comm).writeMsg(org.mockito.Matchers.eq(expected))
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
      test.sendStatus("status", "message")

      verify(test).sendState(Comm.KeyStatusMsg, expectedData);
    }
  }

  describe("#sendState") {
    it("should send a state message with the widget's comm") {
      val msgSupport = spy(MessageSupport(mock[CommWriter]))

      msgSupport.sendState("a", JsString("b"))
      verify(msgSupport).sendState("a", JsString("b"))
    }
  }

  describe("#sendError") {
    it("should send a status error message with the widget's comm") {
      val msgSupport = spy(MessageSupport(mock[CommWriter]))

      msgSupport.sendError("bayud")
      verify(msgSupport).sendStatus(Comm.StatusError, "bayud")
    }
  }

  describe("#sendOk") {
    it("should send a state message with the widget's comm") {
      val msgSupport = spy(MessageSupport(mock[CommWriter]))

      msgSupport.sendOk("goodness")
      verify(msgSupport).sendStatus(Comm.StatusOk, "goodness")
    }
  }
}
