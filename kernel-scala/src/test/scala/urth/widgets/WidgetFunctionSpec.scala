/**
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */

package urth.widgets

import com.ibm.spark.comm.CommWriter
import com.ibm.spark.kernel.protocol.v5.MsgData
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import play.api.libs.json.{JsBoolean, JsString, Json}

class WidgetFunctionSpec extends FunSpec with Matchers with MockitoSugar {

  class TestWidget(comm: CommWriter) extends WidgetFunction(comm)

  class TestWidgetNoInvoke(comm: CommWriter) extends WidgetFunction(comm) {
    override def handleInvoke(msg: MsgData, name: String, limit: Int) = ()
  }

  class TestWidgetNoSignature(comm: CommWriter) extends WidgetFunction(comm) {
    override def sendSignature(name: String) = ()
  }

  describe("WidgetFunction"){
    describe("#handleBackbone") {
      it("should register a function name and send the signature") {
        val test = spy(new TestWidgetNoSignature(mock[CommWriter]))
        val msg = Json.obj(Comm.KeySyncData -> Map(Comm.KeyFunctionName -> "f"))
        test.handleBackbone(msg)
        verify(test).registerFunction("f")
        verify(test).sendSignature("f")
      }

      it("should not register a function when no function name is provided") {
        val test = spy(new TestWidget(mock[CommWriter]))
        val msg = Json.obj(Comm.KeySyncData -> "")
        test.handleBackbone(msg)
        verify(test, times(0)).registerFunction(anyString())
      }

      it("should register a limit") {
        val test = spy(new TestWidget(mock[CommWriter]))
        val msg = Json.obj(Comm.KeySyncData -> Map(Comm.KeyLimit -> 100))
        test.handleBackbone(msg)
        verify(test).registerLimit(100)
      }

      it("should not register a limit when no limit is provided") {
        val test = spy(new TestWidget(mock[CommWriter]))
        val msg = Json.obj(Comm.KeySyncData -> "")
        test.handleBackbone(msg)
        verify(test, times(0)).registerLimit(anyInt())
      }
    }

    describe("#handleCustom") {
      it("should handle an invoke event using the message and current variables") {
        val test = spy(new TestWidgetNoInvoke(mock[CommWriter]))

        test.registerFunction("f")
        test.registerLimit(100)

        val msg = Json.obj(Comm.KeyEvent -> Comm.EventInvoke)
        test.handleCustom(msg)
        verify(test).handleInvoke(msg, "f", 100)
      }

      it("should send the current signature on a sync event") {
        val test = spy(new TestWidgetNoSignature(mock[CommWriter]))

        test.registerFunction("f")

        val msg = Json.obj(Comm.KeyEvent -> Comm.EventSync)
        test.handleCustom(msg)
        verify(test).sendSignature("f")
      }

      it("should not handle an invalid event") {
        val test = spy(new TestWidget(mock[CommWriter]))
        val msg = Json.obj(Comm.KeyEvent -> "asdf")
        test.handleCustom(msg)
        verify(test, times(0)).handleInvoke(any(), anyString(), anyInt())
      }
    }

    describe("#handleInvoke") {
      it("should invoke the function and send the serialized result") {
        val test = spy(new TestWidget(mock[CommWriter]))
        doReturn(Some("result")).when(test).invokeFunc(anyString(), any())

        val args = Json.obj("a" -> 1)
        val msg = Json.obj(Comm.KeyArgs -> args)
        test.handleInvoke(msg, "f", 100)

        verify(test).invokeFunc("f", args)
        verify(test).sendResult(JsString("result"))
      }
    }

    describe("#invokeFunc") {
      it("should invoke the function with the arguments as a map") {
        val test = spy(new TestWidget(mock[CommWriter]))

        val args = Json.obj("a" -> "1")

        doReturn(Some("")).when(test).invokeFunction(anyString(), any())
        test.invokeFunc("f", args) should be(Some(""))
        verify(test).invokeFunction("f", args.as[Map[String, String]])
      }

      it("should invoke the function with any JSON type in the argument map"){
        val test = spy(new TestWidget(mock[CommWriter]))

        val args = Json.obj("a" -> 1, "b" -> "2")

        val stringArgs = Map("a" -> "1", "b" -> "2")
        doReturn(Some("")).when(test).invokeFunction(anyString(), any())
        test.invokeFunc("f", args) should be(Some(""))
        verify(test).invokeFunction("f", stringArgs)
      }
    }

    describe("#sendSignature") {
      it("should send the argument spec with JavaScript types and " +
         "required flags for a valid function") {
        val test = spy(new TestWidget(mock[CommWriter]))

        val spec = Map("a" -> Map(
          "type" -> JsString("String"),
          "required" -> JsBoolean(true)
        ))

        doReturn(Some(spec)).when(test).signature(anyString())
        test.sendSignature("f")

        val expected = Json.toJson(spec)
        verify(test).sendState(Comm.StateSignature, expected)
      }
    }
  }
}