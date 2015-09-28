package urth.widgets

import com.ibm.spark.comm.CommWriter
import com.ibm.spark.kernel.protocol.v5.MsgData
import com.ibm.spark.utils.LogLike
import urth.widgets.util.{MessageSupport, SerializationSupport}

/**
 * A widget that provides an API for setting bound channel variables.
 * @param comm CommWriter used for communication with the front-end.
 */
class WidgetChannels(val comm: CommWriter)
  extends Widget(comm) {

  WidgetChannels.register(this)

  override def handleBackbone(msg: MsgData): Unit = ()

  override def handleCustom(msgContent: MsgData): Unit = ()

}

/**
 * Provides methods for interacting with a single channel.
 * @param comm CommWriter used for communication with the front-end.
 * @param chan Name of the channel.
 */
case class Channel(comm: CommWriter, chan: String)
  extends MessageSupport with SerializationSupport {

  /**
   * Set a property on this channel using the given key and value.
   * @param key
   * @param value
   * @param limit Bounds the output for some serializers, e.g. limits the
   *              number of rows returned for a DataFrame.
   */
  def set(key: String, value: Any, limit: Int = Default.Limit) =
    sendState(comm, chanKey(chan, key), serialize(value, limit))

  private def chanKey(chan: String, key: String): String = s"$chan:$key"

}

/**
 * Provides access to Channel instances that use the most recently created
 * WidgetChannels' comm instance for communication.
 */
object WidgetChannels extends LogLike {

  // Stores the most recently created WidgetChannels widget instance.
  private[widgets] var theChannels: WidgetChannels = _

  /**
   * Provides a Channel instance for interacting with the channel
   * with the given name.
   * @param chan Channel name.
   * @return Channel instance for `chan`.
   */
  def channel(chan: String = Default.Channel): Channel =
    Channel(this.theChannels.comm, chan)

  /**
   * Stores the given widget as the global widget to use for communication.
   * @param widget WidgetChannels widget instance to store.
   */
  private[widgets] def register(widget: WidgetChannels) : Unit = {
    this.theChannels = widget
    logger.debug(s"Registered widget $widget as the global Channels.")
  }

}
