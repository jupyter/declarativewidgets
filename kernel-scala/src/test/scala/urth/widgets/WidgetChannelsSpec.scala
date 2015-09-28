package urth.widgets

import com.ibm.spark.comm.CommWriter
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, FunSpec}
import org.mockito.Mockito._

class WidgetChannelsSpec extends FunSpec with Matchers with MockitoSugar {

  describe("WidgetChannels") {
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
  }
}
