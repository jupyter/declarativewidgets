/**
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */

package declarativewidgets

import declarativewidgets.util.MessageSupport
import org.apache.spark.SharedSparkContext
import org.apache.spark.sql.{SQLContext, DataFrame}
import org.apache.toree.comm.CommWriter
import org.apache.toree.interpreter.Interpreter
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfter, FunSpec, Matchers}
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json

class WidgetDataFrameSpec extends FunSpec with Matchers with MockitoSugar with BeforeAndAfterAll with SharedSparkContext{

  class TestWidget(comm: CommWriter) extends WidgetDataFrame(comm)

  case class TestRow( val name:String, val age: Int )

  object TestRow {
    def from(line: String): TestRow = {
      val f = line.split(",")
      TestRow(f(0), f(1).trim().toInt)
    }
  }

  var df:DataFrame = _

  override def beforeAll() = {
    super.beforeAll()
    val sqlCtx = new SQLContext(sc)
    df = sqlCtx.createDataFrame(Seq("jon, 10", "mary, 15", "bob, 8").map(TestRow.from))
  }

  describe("WidgetDataFrame"){
    describe("#handleBackbone") {
      it("should register a dataframe name") {
        val test = spy(new TestWidget(mock[CommWriter]))
        val msg = Json.obj(Comm.KeySyncData -> Map(Comm.KeyDataFrameName -> "df"))
        val intp = mock[Interpreter]
        doReturn(Some("")).when(intp).read(anyString())
        doReturn(intp).when(test).kernelInterpreter

        test.handleBackbone(msg, mock[MessageSupport])
        verify(test).registerName("df")
      }

      it("should not register a dataframe when no dataframe name is provided") {
        val test = spy(new TestWidget(mock[CommWriter]))
        val msg = Json.obj(Comm.KeySyncData -> "")
        test.handleBackbone(msg, mock[MessageSupport])
        verify(test, times(0)).registerName(anyString())
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

      it("should register a query") {
        val test = spy(new TestWidget(mock[CommWriter]))
        val msg = Json.obj(Comm.KeySyncData -> Map("query" -> "some query"))
        test.handleBackbone(msg, mock[MessageSupport])
        verify(test).registerQuery("some query")
      }
    }

    describe("#handleCustom") {
      it("should handle a sync event using the current variable name and limit") {
        val test = spy(new TestWidget(mock[CommWriter]))

        val msg = Json.obj(Comm.KeyEvent -> Comm.EventSync)

        doNothing().when(test).syncData(any())

        test.handleCustom(msg, mock[MessageSupport])

        verify(test, times(1)).syncData(any())
      }

      it("should not handle an invalid event") {
        val test = spy(new TestWidget(mock[CommWriter]))
        val msg = Json.obj(Comm.KeyEvent -> "asdf")
        test.handleCustom(msg, mock[MessageSupport])
        verify(test, times(0)).syncData(any())
      }
    }

    describe("#syncData") {
      it("should serialize the dataframe, send sync message, and ok message when the DataFrame is present") {
        val test = spy(new TestWidget(mock[CommWriter]))
        val msgSupport = spy(MessageSupport(mock[CommWriter]))

        doReturn(Some(df)).when(test).theDataframe

        test.syncData(msgSupport)
        verify(test).serialize(df, 100)
        verify(test).sendSyncData(any(), org.mockito.Matchers.eq(msgSupport))
        verify(msgSupport).sendOk(any())
      }

      it("should send error message when the dataframe is not present") {
        val test = spy(new TestWidget(mock[CommWriter]))
        val msgSupport = spy(MessageSupport(mock[CommWriter]))

        doReturn(None).when(test).theDataframe

        test.syncData(msgSupport)
        verify(test, times(0)).serialize(any(), anyInt())
        verify(test, times(0)).sendSyncData(any(), org.mockito.Matchers.eq(msgSupport))
        verify(msgSupport).sendError(any())
      }

      it("should send error message when the query is not parseable") {
        val test = spy(new TestWidget(mock[CommWriter]))
        test.query = "not parseable"

        val msgSupport = spy(MessageSupport(mock[CommWriter]))

        doReturn(Some(df)).when(test).theDataframe

        test.syncData(msgSupport)
        verify(test, times(0)).serialize(any(), anyInt())
        verify(test, times(0)).sendSyncData(any(), org.mockito.Matchers.eq(msgSupport))
        verify(msgSupport).sendError(any())
      }

      it("should send error message when the variableName does not map a DataFrame") {
        val test = spy(new TestWidget(mock[CommWriter]))
        test.query = "not parseable"

        val msgSupport = spy(MessageSupport(mock[CommWriter]))

        doReturn(Some("not a dataframe")).when(test).theDataframe

        test.syncData(msgSupport)
        verify(test, times(0)).serialize(any(), anyInt())
        verify(test, times(0)).sendSyncData(any(), org.mockito.Matchers.eq(msgSupport))
        verify(msgSupport).sendError(any())
      }

    }
  }
}