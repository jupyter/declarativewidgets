/**
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */

package declarativewidgets

import declarativewidgets.util.MessageSupport
import org.apache.toree.comm.CommWriter
import org.apache.toree.kernel.protocol.v5.MsgData
import org.mockito.ArgumentCaptor
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, FunSpec}
import play.api.libs.json.{JsValue, JsObject, JsString, Json}
import org.mockito.Mockito._
import org.mockito.Matchers._

class WidgetSpec extends FunSpec with Matchers with MockitoSugar {

  class TestWidget(comm: CommWriter) extends Widget(comm) {
    override def handleBackbone(msg: MsgData, msgSupport: MessageSupport): Unit = ()

    override def handleCustom(msg: MsgData, msgSupport: MessageSupport): Unit = ()
  }

  describe("Widget"){
    describe("#handleMsg") {
      it("should handle backbone message when the method is backbone") {
        val test = spy(new TestWidget(mock[CommWriter]))
        val msg = Json.obj("method" -> "backbone")
        test.handleMsg(msg, mock[CommWriter])

        verify(test).handleBackbone(org.mockito.Matchers.eq(msg), any[MessageSupport])
      }

      it("should handle request_state message when the method is request_state") {
        val test = spy(new TestWidget(mock[CommWriter]))
        val msg = Json.obj("method" -> "request_state")
        test.handleMsg(msg, mock[CommWriter])

        verify(test).handleRequestState(org.mockito.Matchers.eq(msg), any[MessageSupport])
      }

      it("should handle custom message and pass the content when the " +
         "method is custom and contains content") {
        val test = spy(new TestWidget(mock[CommWriter]))
        val msg = Json.obj("method" -> "custom", "content" -> "c")
        test.handleMsg(msg, mock[CommWriter])

        verify(test).handleCustom(org.mockito.Matchers.eq(JsString("c")), any[MessageSupport])
      }

      it("should do nothing when the message is custom and does not " +
         "contain content") {
        val test = spy(new TestWidget(mock[CommWriter]))
        val msg = Json.obj("method" -> "custom")
        test.handleMsg(msg, mock[CommWriter])

        verify(test, times(0)).handleCustom(any(), any[MessageSupport])
      }

      it("should do nothing when the message method is invalid") {
        val test = spy(new TestWidget(mock[CommWriter]))
        val msg = Json.obj()
        test.handleMsg(msg, mock[CommWriter])

        verify(test, times(0)).handleCustom(any(), any[MessageSupport])
        verify(test, times(0)).handleBackbone(any(), any[MessageSupport])
      }
    }

    describe("#handleRequestState") {
      it("should send an empty state by default"){
        val comm = mock[CommWriter]
        val test = spy(new TestWidget(comm))

        val msgSupport = spy(MessageSupport(mock[CommWriter]))

        test.handleRequestState(mock[MsgData], msgSupport)

        val stateCaptor = ArgumentCaptor.forClass(classOf[Map[String,JsValue]])

        verify(msgSupport).sendState(stateCaptor.capture())

        stateCaptor.getValue shouldBe empty
      }
    }

    describe("#createWidgetInstance") {
      it("should create a function widget when given the function class name") {
        Widget.createWidgetInstance(WidgetClass.Function, mock[CommWriter]).getClass should
         be(classOf[WidgetFunction])
      }

      it("should create a dataframe widget when given the function class name") {
        Widget.createWidgetInstance(WidgetClass.DataFrame, mock[CommWriter]).getClass should
          be(classOf[WidgetDataFrame])
      }
    }

    describe("#openCallback") {
      it("should create a register a new instance by ID in the widgets map") {
        val msg = Json.obj(Comm.KeyWidgetClass -> WidgetClass.Function)

        Widget.openCallback(mock[CommWriter], "ID", "", msg)

        Widget.widgets.keys.toList.contains("ID") should be (true)

        Widget.widgets("ID").getClass should be (classOf[WidgetFunction])
      }
    }

    describe("#msgCallback") {
      it("should handle a comm message") {

        val widget = spy(new WidgetFunction(mock[CommWriter]))
        Widget.widgets += ("ID" -> widget)

        val msg = Json.obj()

        Widget.msgCallback(mock[CommWriter], "ID", msg)

        verify(widget).handleMsg(org.mockito.Matchers.eq(msg), any[CommWriter])
      }
    }
  }
}