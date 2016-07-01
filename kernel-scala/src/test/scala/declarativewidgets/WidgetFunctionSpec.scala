/**
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */

package declarativewidgets

import declarativewidgets.util.MessageSupport
import org.apache.toree.comm.CommWriter
import org.apache.toree.kernel.protocol.v5.MsgData
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import play.api.libs.json.{JsBoolean, JsString, Json}

import scala.util.{Failure, Success}

class WidgetFunctionSpec extends FunSpec with Matchers with MockitoSugar {

  class TestWidget(comm: CommWriter) extends WidgetFunction(comm)

  class TestWidgetNoInvoke(comm: CommWriter) extends WidgetFunction(comm) {
    override def handleInvoke(msg: MsgData, name: String, limit: Int, msgSupport:MessageSupport) = ()
  }

  class TestWidgetNoSignature(comm: CommWriter) extends WidgetFunction(comm) {
    override def sendSignature(name: String, msgSupport:MessageSupport) = Right(())
  }

  describe("WidgetFunction"){
    describe("#handleBackbone") {
      it("should register a function name and send the signature") {
        val test = spy(new TestWidgetNoSignature(mock[CommWriter]))
        val msgSupport = spy(MessageSupport(mock[CommWriter]))

        val msg = Json.obj(Comm.KeySyncData -> Map(Comm.KeyFunctionName -> "f"))
        test.handleBackbone(msg, msgSupport)
        verify(test).registerFunction("f", msgSupport)
        verify(test).sendSignature("f", msgSupport)
      }

      it("should not register a function when no function name is provided") {
        val test = spy(new TestWidget(mock[CommWriter]))

        val msg = Json.obj(Comm.KeySyncData -> "")
        test.handleBackbone(msg, mock[MessageSupport])
        verify(test, times(0)).registerFunction(anyString(), any[MessageSupport])
      }

      it("should register a limit") {
        val test = spy(new TestWidget(mock[CommWriter]))

        val msg = Json.obj(Comm.KeySyncData -> Map(Comm.KeyLimit -> 100))
        test.handleBackbone(msg, mock[MessageSupport])
        verify(test).registerLimit(100)
      }

      it("should not register a limit when no limit is provided") {
        val test = spy(new TestWidget(mock[CommWriter]))
        val msg = Json.obj(Comm.KeySyncData -> "")
        test.handleBackbone(msg, mock[MessageSupport])
        verify(test, times(0)).registerLimit(anyInt())
      }
    }

    describe("#handleCustom") {
      it("should handle an invoke event using the message and current variables") {
        val test = spy(new TestWidgetNoInvoke(mock[CommWriter]))
        doReturn(Some(Map())).when(test).signature(anyString())
        test.registerFunction("f", mock[MessageSupport])
        test.registerLimit(100)

        val msg = Json.obj(Comm.KeyEvent -> Comm.EventInvoke)
        val msgSupport = mock[MessageSupport]
        test.handleCustom(msg, msgSupport)

        verify(test).handleInvoke(msg, "f", 100, msgSupport)
      }

      it("should send the current signature on a sync event") {
        val test = spy(new TestWidgetNoSignature(mock[CommWriter]))

        test.registerFunction("f", mock[MessageSupport])

        val msg = Json.obj(Comm.KeyEvent -> Comm.EventSync)
        val msgSupport = mock[MessageSupport]

        test.handleCustom(msg, msgSupport)

        // once from registerFunction, once from handleCustom
        verify(test).sendSignature(org.mockito.Matchers.eq("f"), org.mockito.Matchers.eq(msgSupport))
      }

      it("should not handle an invalid event") {
        val test = spy(new TestWidget(mock[CommWriter]))
        val msg = Json.obj(Comm.KeyEvent -> "asdf")

        val msgSupport = mock[MessageSupport]

        test.handleCustom(msg, msgSupport)
        verify(test, times(0)).handleInvoke(any(), anyString(), anyInt(), org.mockito.Matchers.eq(msgSupport))
      }
    }

    describe("#handleInvoke") {
      it("should invoke the function and send the serialized result") {
        val test = spy(new TestWidget(mock[CommWriter]))
        doReturn(Success(JsString("result"))).when(test).invokeFunc(anyString(), any(), anyInt())

        val msgSupport = mock[MessageSupport]

        val args = Json.obj("a" -> 1)
        val msg = Json.obj(Comm.KeyArgs -> args)
        test.handleInvoke(msg, "f", 100, msgSupport)

        verify(test).invokeFunc("f", args, 100)
        verify(test).sendResult(JsString("result"), msgSupport)
      }

      it("should send a status ok message when the invocation succeeds"){
        val test = spy(new TestWidget(mock[CommWriter]))
        doReturn(Success(JsString("result"))).when(test).invokeFunc(anyString(), any(), anyInt())

        val msgSupport = spy(MessageSupport(mock[CommWriter]))

        val args = Json.obj("a" -> 1)
        val msg = Json.obj(Comm.KeyArgs -> args)
        test.handleInvoke(msg, "f", 100, msgSupport)

        verify(msgSupport).sendOk()
      }

      it("should send a status error message when the invocation fails"){
        val test = spy(new TestWidget(mock[CommWriter]))
        doReturn(Failure(new Exception)).when(test).invokeFunc(anyString(), any(), anyInt())
        val args = Json.obj("a" -> 1)
        val msg = Json.obj(Comm.KeyArgs -> args)

        val msgSupport = spy(MessageSupport(mock[CommWriter]))

        test.handleInvoke(msg, "f", 100, msgSupport)

        verify(msgSupport).sendError(anyString())
      }

      it("should send a status error message when the msg does not contain arguments"){
        val test = spy(new TestWidget(mock[CommWriter]))
        val msg = Json.obj()
        val msgSupport = spy(MessageSupport(mock[CommWriter]))

        test.handleInvoke(msg, "f", 100, msgSupport)

        verify(msgSupport).sendError(anyString())
      }
    }

    describe("#invokeFunc") {
      it("should invoke the function with the arguments as a map") {
        val test = spy(new TestWidget(mock[CommWriter]))

        val args = Json.obj("a" -> "1")

        val output = Success("")
        doReturn(output).when(test).invokeFunction(anyString(), any())
        val result = test.invokeFunc("f", args)
        result.isSuccess should be(true)
        result.get should be(JsString(output.get))
        verify(test).invokeFunction("f", args.as[Map[String, String]])
      }

      it("should invoke the function with any JSON type in the argument map"){
        val test = spy(new TestWidget(mock[CommWriter]))

        val args = Json.obj("a" -> 1, "b" -> "2")

        val output = Success("")
        val stringArgs = Map("a" -> "1", "b" -> "2")
        doReturn(output).when(test).invokeFunction(anyString(), any())
        val result = test.invokeFunc("f", args)
        result.isSuccess should be(true)
        result.get should be(JsString(output.get))
        verify(test).invokeFunction("f", stringArgs)
      }
    }

    describe("#registerFunction") {
      it("should try to send the signature of the registered function"){
        val test = spy(new TestWidgetNoSignature(mock[CommWriter]))

        val msgSupport = spy(MessageSupport(mock[CommWriter]))

        test.registerFunction("foo", msgSupport)
        verify(test).sendSignature("foo", msgSupport)
      }

      it("should send a status ok if the signature sending succeeded"){
        val test = spy(new TestWidget(mock[CommWriter]))
        doReturn(Right(())).when(test).sendSignature(anyString(), any[MessageSupport])
        val msgSupport = spy(MessageSupport(mock[CommWriter]))

        test.registerFunction("foo", msgSupport)
        verify(msgSupport).sendOk()
      }

      it("should send a status error if the signature sending failed"){
        val test = spy(new TestWidget(mock[CommWriter]))

        val msgSupport = spy(MessageSupport(mock[CommWriter]))

        doReturn(Left(("uh oh"))).when(test).sendSignature(anyString(), any[MessageSupport])
        test.registerFunction("foo", msgSupport)
        verify(msgSupport).sendError("uh oh")
      }
    }

    describe("#sendSignature") {
      it("should send the argument spec with JavaScript types and " +
         "required flags for a valid function, and return Right") {
        val test = spy(new TestWidget(mock[CommWriter]))

        val spec = Map("a" -> Map(
          "type" -> JsString("String"),
          "required" -> JsBoolean(true)
        ))

        doReturn(Some(spec)).when(test).signature(anyString())

        val msgSupport = spy(MessageSupport(mock[CommWriter]))

        val result = test.sendSignature("f", msgSupport)

        val expected = Json.toJson(spec)
        verify(msgSupport).sendState(Comm.StateSignature, expected)
        result.isRight should be(true)
      }

      it("should return Left when signature inference fails"){
        val test = spy(new TestWidget(mock[CommWriter]))

        doReturn(None).when(test).signature(anyString())
        val result = test.sendSignature("f", mock[MessageSupport])
        result.isLeft should be(true)
      }

    }
  }
}