package urth.widgets

import com.ibm.spark.comm.CommWriter
import com.ibm.spark.kernel.protocol.v5.MsgData
import com.ibm.spark.utils.LogLike
import play.api.libs.json.JsValue
import urth.widgets.util.{StandardFunctionSupport, MessageSupport, SerializationSupport}

/**
 * A widget that provides an API for setting bound channel variables.
 * @param comm CommWriter used for communication with the front-end.
 */
class WidgetChannels(val comm: CommWriter)
  extends Widget(comm) with StandardFunctionSupport {

  WidgetChannels.register(this)

  override def handleBackbone(msg: MsgData): Unit = ()

  /**
   * Handles a Comm Message whose method is custom.
   * @param msgContent The content field of the Comm Message.
   */
  override def handleCustom(msgContent: MsgData): Unit = {
    logger.debug(s"Handling custom message ${msgContent}...")
    (msgContent \ Comm.KeyEvent).asOpt[String] match {
      case Some(Comm.EventChange) => handleChange(msgContent) match {
        case Some(u) => ()
        case None => logger.trace(s"Failed to execute watch handler for " +
          s"${msgContent} with handlers ${WidgetChannels.chanHandlers}"
        )
      }
      case Some(evt) =>
        logger.warn(s"Unhandled custom event ${evt}.")
      case None =>
        logger.warn("No event value in custom comm message.")
    }
  }

  /**
   * Execute the handler corresponding to the channel and variable found in
   * the message using the old and new values found in the message.
   * @param msgContent Message contents.
   * @return Some(Unit) or None if an error occurred.
   */
  private[widgets] def handleChange(msgContent: MsgData): Option[Unit] = for {
    chan <- (msgContent \ Comm.ChangeData \ Comm.ChangeChannel).asOpt[String]
    name <- (msgContent \ Comm.ChangeData \ Comm.ChangeName).asOpt[String]
    oldVal   <- (msgContent \ Comm.ChangeData \ Comm.ChangeOldVal).asOpt[JsValue]
    newVal   <- (msgContent \ Comm.ChangeData \ Comm.ChangeNewVal).asOpt[JsValue]
    handlers <- WidgetChannels.chanHandlers.get(chan)
    handler  <- handlers.get(name)
    res      <- invokeWatchHandler(oldVal, newVal, handler)
  } yield res

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
  def set(key: String, value: Any, limit: Int = Default.Limit): Unit =
    sendState(comm, chanKey(chan, key), serialize(value, limit))

  /**
   * Watch the given variable on this channel for changes, and execute the
   * given handler when a change occurs.
   * @param variable Name of the variable to watch.
   * @param handler Handler to execute when a change to `variable` occurs.
   */
  def watch(variable: String, handler: WatchHandler): Unit =
    WidgetChannels.watch(this.chan, variable, handler)

  private def chanKey(chan: String, key: String): String = s"$chan:$key"

}

/**
 * Provides access to Channel instances that use the most recently created
 * WidgetChannels' comm instance for communication.
 */
object WidgetChannels extends LogLike {

  // Stores the most recently created WidgetChannels widget instance.
  private[widgets] var theChannels: WidgetChannels = _

  // Maps channel name to a map of variable name to handler.
  private[widgets] var chanHandlers: Map[String, Map[String, WatchHandler]] =
    Map()

  /**
   * Provides a Channel instance for interacting with the channel
   * with the given name.
   * @param chan Channel name.
   * @return Channel instance for `chan`.
   */
  def channel(chan: String = Default.Channel): Channel =
    Channel(this.theChannels.comm, chan)

  /**
   * Register the handler to execute when a change occurs to the given
   * variable name on the given channel.
   * @param chan Channel name.
   * @param variable Variable name to watch for changes.
   * @param handler Handler to register.
   */
  private[widgets] def watch(
     chan: String, variable: String, handler: WatchHandler
  ): Unit = {
    logger.trace(s"Registering handler $handler for $variable on channel $chan")
    chanHandlers = chanHandlers +
      (chan -> (chanHandlers.getOrElse(chan, Map()) + (variable -> handler)))
  }

  /**
   * Stores the given widget as the global widget to use for communication.
   * @param widget WidgetChannels widget instance to store.
   */
  private[widgets] def register(widget: WidgetChannels) : Unit = {
    this.theChannels = widget
    logger.debug(s"Registered widget $widget as the global Channels.")
  }

}
