package urth.widgets

import com.ibm.spark.comm.CommWriter
import com.ibm.spark.kernel.protocol.v5._
import org.mockito.Matchers._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, FunSpec}
import org.mockito.Mockito._
import play.api.libs.json._

class WidgetChannelsSpec extends FunSpec with Matchers with MockitoSugar {

  class TestWidget(comm: CommWriter) extends WidgetChannels(comm)

  class TestWidgetNoChange(comm: CommWriter) extends WidgetChannels(comm) {
    override def handleChange(msgContent: MsgData) = Right(Unit)
  }

  describe("WidgetChannels") {

    describe("#watch") {
      it("integration: should execute the handler when the watched variable changes") {

        val test = new TestWidget(mock[CommWriter])

        val chan = "the_chan"
        val name = "x"

        var arg1: Int = -1

        var arg2: Int = -1

        val handler = (x: Int, y: Int) => {arg1 = x; arg2 = y}

        WidgetChannels.channel(chan).watch(name, handler)

        val msg = Json.obj(
          Comm.KeyEvent -> Comm.EventChange,
          Comm.ChangeData -> Map(
            Comm.ChangeChannel -> JsString(chan),
            Comm.ChangeName -> JsString(name),
            Comm.ChangeOldVal -> JsNumber(0),
            Comm.ChangeNewVal -> JsNumber(1)
          )
        )

        test.handleChange(msg)

        arg1 should equal(0)
        arg2 should equal(1)
      }
    }

    describe("#handleCustom") {
      it("should handle a change event using the message contents") {
        val test = spy(new TestWidgetNoChange(mock[CommWriter]))

        val msg = Json.obj(Comm.KeyEvent -> Comm.EventChange)
        test.handleCustom(msg)
        verify(test).handleChange(msg)
      }

      it("should not handle an invalid event") {
        val test = spy(new TestWidget(mock[CommWriter]))
        val msg = Json.obj(Comm.KeyEvent -> "asdf")
        test.handleCustom(msg)
        verify(test, times(0)).handleChange(any())
      }

      it("should send a status ok message if handling the change succeeded"){
        val test = spy(new TestWidget(mock[CommWriter]))
        doReturn(Right(())).when(test).handleChange(any())
        val msg = Json.obj(Comm.KeyEvent -> Comm.EventChange)
        test.handleCustom(msg)
        verify(test).sendOk()
      }

      it("should send a status error message if handling the change fails"){
        val test = spy(new TestWidget(mock[CommWriter]))
        doReturn(Left("uh oh")).when(test).handleChange(any())
        val msg = Json.obj(Comm.KeyEvent -> Comm.EventChange)
        test.handleCustom(msg)
        verify(test).sendError("uh oh")
      }
    }

    describe("#handleChange") {
      val chan = "c"
      val name = "x"

      it ("should invoke the watch handler for the given channel and name " +
         "with the given argument values") {
        val old = 1
        val noo = 2
        val msg = Json.obj(
          Comm.ChangeData -> Map(
            Comm.ChangeChannel -> JsString(chan),
            Comm.ChangeName -> JsString(name),
            Comm.ChangeOldVal -> JsNumber(old),
            Comm.ChangeNewVal -> JsNumber(noo)
          )
        )

        var executed = false
        val handler = (x: Int, y: Int) => executed = true; ()
        WidgetChannels.watch(chan, name, handler)
        val wid = spy(new TestWidget(mock[CommWriter]))
        wid.handleChange(msg)

        executed should be(true)
      }

      it ("should auto convert numeric types to fit the type signature") {
        val old = 1
        val noo = 2.0
        val msg = Json.obj(
          Comm.ChangeData -> Map(
            Comm.ChangeChannel -> JsString(chan),
            Comm.ChangeName -> JsString(name),
            Comm.ChangeOldVal -> JsNumber(old),
            Comm.ChangeNewVal -> JsNumber(noo)
          )
        )

        var executed = false
        val handler = (x: Double, y: Double) => executed = true; ()
        WidgetChannels.watch(chan, name, handler)
        val wid = spy(new TestWidget(mock[CommWriter]))
        wid.handleChange(msg)

        executed should be(true)
      }

      it ("should return Right when the channel is not registered") {
        val old = 1
        val noo = 2
        val msg = Json.obj(
          Comm.ChangeData -> Map(
            Comm.ChangeChannel -> JsString("not registered"),
            Comm.ChangeName -> JsString(name),
            Comm.ChangeOldVal -> JsNumber(old),
            Comm.ChangeNewVal -> JsNumber(noo)
          )
        )

        val wid = spy(new TestWidget(mock[CommWriter]))
        wid.handleChange(msg).isRight should be(true)
      }

      it ("should return Right when the name is not registered") {
        val old = 1
        val noo = 2
        val msg = Json.obj(
          Comm.ChangeData -> Map(
            Comm.ChangeChannel -> JsString(chan),
            Comm.ChangeName -> JsString("not registered"),
            Comm.ChangeOldVal -> JsNumber(old),
            Comm.ChangeNewVal -> JsNumber(noo)
          )
        )

        var executed = false
        val handler = (x: Int, y: Int) => executed = true; ()
        WidgetChannels.watch(chan, "DNE", handler)
        val wid = spy(new TestWidget(mock[CommWriter]))
        wid.handleChange(msg).isRight should be(true)

        executed should be(false)
      }

      it ("should fail (return Left) if oldVal is undefined") {
        val noo = 2
        val msg = Json.obj(
          Comm.ChangeData -> Map(
            Comm.ChangeChannel -> JsString(chan),
            Comm.ChangeName -> JsString(name),
            Comm.ChangeOldVal -> JsNull,
            Comm.ChangeNewVal -> JsNumber(noo)
          )
        )

        var executed = false
        val handler = (x: Int, y: Int) => executed = true; ()
        WidgetChannels.watch(chan, name, handler)
        val wid = spy(new TestWidget(mock[CommWriter]))
        wid.handleChange(msg).isLeft should be(true)

      }

      it ("should return Left when invocation fails") {
        val old = 1
        val noo = 2
        val msg = Json.obj(
          Comm.ChangeData -> Map(
            Comm.ChangeChannel -> JsString(chan),
            Comm.ChangeName -> JsString(name),
            Comm.ChangeOldVal -> JsNumber(old),
            Comm.ChangeNewVal -> JsNumber(noo)
          )
        )

        val handler = (x: Int, y: Int) => {val explosion = 1 / 0; ()}
        WidgetChannels.watch(chan, name, handler)
        val wid = spy(new TestWidget(mock[CommWriter]))
        wid.handleChange(msg).isLeft should be(true)
      }

      it ("should return Left if argument types don't match handler types") {
        val old = 1
        val noo = 2
        val msg = Json.obj(
          Comm.ChangeData -> Map(
            Comm.ChangeChannel -> JsString(chan),
            Comm.ChangeName -> JsString(name),
            Comm.ChangeOldVal -> JsNumber(old),
            Comm.ChangeNewVal -> JsNumber(noo)
          )
        )

        val handler = (x: String, y: String) => ()
        WidgetChannels.watch(chan, name, handler)
        val wid = spy(new TestWidget(mock[CommWriter]))
        wid.handleChange(msg).isLeft should be(true)
      }

      it ("should return Left if the message format is invalid") {
        val msg = Json.obj()
        val handler = (x: Int, y: Int) => ()
        WidgetChannels.watch(chan, name, handler)
        val wid = spy(new TestWidget(mock[CommWriter]))
        wid.handleChange(msg).isLeft should be(true)
      }
    }

    describe("#watch") {
      it ("should add a handler to the map for the given channel and name") {
        val chan = "chan2"
        val name = "n"
        val handler = (x: String, y: String) => ()
        WidgetChannels.watch(chan, name, handler)
        WidgetChannels.chanHandlers(chan)(name) should be(handler)
      }

      it ("should overwrite an existing entry with the new handler") {
        val chan = "c1"
        val name = "n"
        val handler = (x: String, y: String) => ()
        val handler2 = (x: String, y: String) => ()
        WidgetChannels.watch(chan, name, handler)
        WidgetChannels.watch(chan, name, handler2)
        WidgetChannels.chanHandlers(chan)(name) should be(handler2)
      }
    }

    describe("#channel") {
      it ("should give a Channel object for the default channel using the " +
          "registered widget's comm when no channel argument is provided") {
        val comm = mock[CommWriter]
        val widget = mock[WidgetChannels]
        doReturn(comm).when(widget).comm
        WidgetChannels.register(widget)
        WidgetChannels.channel().chan should be(Default.Channel)
        WidgetChannels.channel().comm should be(comm)

      }
      it ("should give a Channel object for the given channel using the " +
        "registered widget's comm") {
        val comm = mock[CommWriter]
        val widget = mock[WidgetChannels]
        doReturn(comm).when(widget).comm
        WidgetChannels.register(widget)
        WidgetChannels.channel("foo").chan should be("foo")
        WidgetChannels.channel("foo").comm should be(comm)
      }
    }

    describe("init") {
      it ("should register the widget instance when created") {
        val comm = mock[CommWriter]
        val widget = new WidgetChannels(comm)
        WidgetChannels.theChannels should be (widget)
      }
    }

    describe("#parseMessage") {
      val chan = "c"
      val name = "x"

      it ("should give channel, name, oldVal, newVal for a valid message"){
        val old = 0
        val noo = 1

        val msg = Json.obj(
          Comm.ChangeData -> Map(
            Comm.ChangeChannel -> JsString(chan),
            Comm.ChangeName -> JsString(name),
            Comm.ChangeOldVal -> JsNumber(old),
            Comm.ChangeNewVal -> JsNumber(noo)
          )
        )

        val expected = Some((chan, name, JsNumber(old), JsNumber(noo)))

        val test = new TestWidget(mock[CommWriter])
        test.parseMessage(msg) should be (expected)
      }

      it ("should give return None given an incomplete message"){
        val noo = 1

        val msg = Json.obj(
          Comm.ChangeData -> Map(
            Comm.ChangeChannel -> JsString(chan),
            Comm.ChangeNewVal -> JsNumber(noo)
          )
        )

        val expected = None

        val test = new TestWidget(mock[CommWriter])
        test.parseMessage(msg) should be (expected)
      }

      it ("should give return None if channel or name are not strings"){
        val old = 0
        val noo = 1

        val msg = Json.obj(
          Comm.ChangeData -> Map(
            Comm.ChangeChannel -> JsNumber(0),
            Comm.ChangeName -> JsNumber(1),
            Comm.ChangeOldVal -> JsNumber(old),
            Comm.ChangeNewVal -> JsNumber(noo)
          )
        )

        val expected = None

        val test = new TestWidget(mock[CommWriter])
        test.parseMessage(msg) should be (expected)
      }
    }

    describe("#getHandler") {
      it("should retrieve a registered handler"){
        val chan = "c"
        val name = "n"
        val handler = (x: Int, y: Int) => ()
        Channel(mock[CommWriter], chan).watch(name, handler)

        val test = new TestWidget(mock[CommWriter])
        test.getHandler(chan, name) should be (Some(handler))
      }

      it("should return none if the requested handler does not exist") {
        val test = new TestWidget(mock[CommWriter])
        test.getHandler("", "") should be (None)
      }
    }
  }
}
