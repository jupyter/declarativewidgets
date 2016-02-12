/**
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */

package urth.widgets

import org.apache.toree.comm.CommWriter
import org.apache.toree.kernel.protocol.v5.MsgData
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, FunSpec}
import play.api.libs.json.{JsString, Json}
import org.mockito.Mockito._
import org.mockito.Matchers._

class WidgetSpec extends FunSpec with Matchers with MockitoSugar {

  class TestWidget(comm: CommWriter) extends Widget(comm) {
    override def handleBackbone(msg: MsgData): Unit = ()

    override def handleCustom(msg: MsgData): Unit = ()
  }

  describe("Widget"){
    describe("#handleMsg") {
      it("should handle backbone message when the method is backbone") {
        val test = spy(new TestWidget(mock[CommWriter]))
        val msg = Json.obj("method" -> "backbone")
        test.handleMsg(msg)

        verify(test).handleBackbone(msg)
      }

      it("should handle custom message and pass the content when the " +
         "method is custom and contains content") {
        val test = spy(new TestWidget(mock[CommWriter]))
        val msg = Json.obj("method" -> "custom", "content" -> "c")
        test.handleMsg(msg)

        verify(test).handleCustom(JsString("c"))
      }

      it("should do nothing when the message is custom and does not " +
         "contain content") {
        val test = spy(new TestWidget(mock[CommWriter]))
        val msg = Json.obj("method" -> "custom")
        test.handleMsg(msg)

        verify(test, times(0)).handleCustom(any())
      }

      it("should do nothing when the message method is invalid") {
        val test = spy(new TestWidget(mock[CommWriter]))
        val msg = Json.obj()
        test.handleMsg(msg)

        verify(test, times(0)).handleCustom(any())
        verify(test, times(0)).handleBackbone(any())
      }
    }

    describe("#sendState") {
      it("should send a state message with the widget's comm") {
        val comm = mock[CommWriter]
        val test = spy(new TestWidget(comm))
        test.sendState("a", JsString("b"))
        verify(test).sendState(comm, "a", JsString("b"))
      }
    }

    describe("#sendError") {
      it("should send a status error message with the widget's comm") {
        val comm = mock[CommWriter]
        val test = spy(new TestWidget(comm))
        test.sendError("bayud")
        verify(test).sendStatus(comm, Comm.StatusError, "bayud")
      }
    }

    describe("#sendOk") {
      it("should send a state message with the widget's comm") {
        val comm = mock[CommWriter]
        val test = spy(new TestWidget(comm))
        test.sendOk("goodness")
        verify(test).sendStatus(comm, Comm.StatusOk, "goodness")
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

        verify(widget).handleMsg(msg)
      }
    }
  }
}