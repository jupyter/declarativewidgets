/**
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */

package urth.widgets

import com.ibm.spark.comm.CommWriter
import com.ibm.spark.interpreter.Interpreter
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json

class WidgetDataFrameSpec extends FunSpec with Matchers with MockitoSugar {

  class TestWidget(comm: CommWriter) extends WidgetDataFrame(comm)

  describe("WidgetDataFrame"){
    describe("#handleBackbone") {
      it("should register a dataframe name") {
        val test = spy(new TestWidget(mock[CommWriter]))
        val msg = Json.obj(Comm.KeySyncData -> Map(Comm.KeyDataFrameName -> "df"))
        test.handleBackbone(msg)
        verify(test).registerName("df")
      }

      it("should not register a dataframe when no dataframe name is provided") {
        val test = spy(new TestWidget(mock[CommWriter]))
        val msg = Json.obj(Comm.KeySyncData -> "")
        test.handleBackbone(msg)
        verify(test, times(0)).registerName(anyString())
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
      it("should handle a sync event using the current variable name and limit") {
        val test = spy(new TestWidget(mock[CommWriter]))

        test.registerName("df")
        test.registerLimit(100)

        val msg = Json.obj(Comm.KeyEvent -> Comm.EventSync)

        val df = ""
        val intp = mock[Interpreter]
        doReturn(Some(df)).when(intp).read(anyString())
        doReturn(intp).when(test).kernelInterpreter

        test.handleCustom(msg)
        verify(test).handleSync(test.variableName, test.limit)
      }

      it("should not handle an invalid event") {
        val test = spy(new TestWidget(mock[CommWriter]))
        val msg = Json.obj(Comm.KeyEvent -> "asdf")
        test.handleCustom(msg)
        verify(test, times(0)).handleSync(anyString(), anyInt())
      }
    }

    describe("#handleSync") {
      it("should serialize the dataframe and send a message when the DataFrame is present") {
        val test = spy(new TestWidget(mock[CommWriter]))
        val msg = Json.obj(Comm.KeyEvent -> Comm.EventSync)

        val df = ""
        val intp = mock[Interpreter]
        doReturn(Some(df)).when(intp).read(anyString())
        doReturn(intp).when(test).kernelInterpreter

        test.handleSync("df", 100)
        verify(test).serialize(df, 100)
        verify(test).sendSyncData(any())
      }

      it("should do nothing when the dataframe is not present") {
        val test = spy(new TestWidget(mock[CommWriter]))
        val msg = Json.obj(Comm.KeyEvent -> Comm.EventSync)

        val df = ""
        val intp = mock[Interpreter]
        doReturn(None).when(intp).read("df")
        doReturn(intp).when(test).kernelInterpreter

        test.handleSync("df", 100)
        verify(test, times(0)).serialize(any(), anyInt())
        verify(test, times(0)).sendSyncData(any())
      }
    }
  }
}