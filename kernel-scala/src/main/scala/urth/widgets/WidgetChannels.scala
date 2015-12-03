package urth.widgets

import com.ibm.spark.comm.CommWriter
import com.ibm.spark.kernel.protocol.v5.MsgData
import com.ibm.spark.utils.LogLike
import play.api.libs.json.{JsNull, JsValue}
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
        case Right(u)  => sendOk()
        case Left(msg) => sendError(msg)
      }
      case Some(evt) => logger.warn(s"Unhandled custom event ${evt}.")
      case None => logger.warn("No event value in custom comm message.")
    }
  }

  /**
   * Execute the handler corresponding to the channel and variable found in
   * the message using the old and new values found in the message.
   * @param msgContent Message contents.
   * @return Right(Unit) or Left(error_message) if an error occurred.
   */
  private[widgets] def handleChange(msgContent: MsgData): Either[String, Unit] =
    parseMessage(msgContent) match {
      case Some((chan, name, oldVal, newVal)) =>
        getHandler(chan, name) match {
          case Some(handler) =>
            invokeWatchHandler(oldVal, newVal, handler).toRight(s"Error " +
              s"invoking watch handler for variable $name on channel $chan"
            )
          case None =>
            logger.warn(s"No watch handler for variable $name on channel $chan")
            Right(())
        }
      case None =>
        logger.warn(s"Could not parse change message $msgContent")
        Left(s"Could not parse change message $msgContent")
    }

  /**
   * Parses the fields necessary to invoke a watch handler from the message.
   * @param msgContent Message to parse.
   * @return Tuple of field values, or None if parsing fails.
   */
  private[widgets] def parseMessage(
    msgContent: MsgData
  ): Option[(String, String, JsValue, JsValue)] = for {
      chan <- (msgContent \ Comm.ChangeData \ Comm.ChangeChannel).asOpt[String]
      name <- (msgContent \ Comm.ChangeData \ Comm.ChangeName).asOpt[String]
      oldVal <- parseOldVal(msgContent)
      newVal <- (msgContent \ Comm.ChangeData \ Comm.ChangeNewVal).asOpt[JsValue]
    } yield (chan, name, oldVal, newVal)

  /**
   * Allow for a non-existent `old_val` by returning JsNull if it isn't present.
   * @param msgContent Message to parse.
   * @return Some(old_val) or Some(JsNull) if old_val isn't present.
   */
  private[widgets] def parseOldVal(msgContent: MsgData): Option[JsValue] = {
    (msgContent \ Comm.ChangeData \ Comm.ChangeOldVal).asOpt[JsValue] match {
      case s@Some(_) => s
      case None => Some(JsNull)
    }
  }

  /**
   * Retrieves the registered WatchHandler for the given channel and name.
   * @param chan channel
   * @param name variable name
   * @return WatchHandler or None if no handler is found.
   */
  private[widgets] def getHandler(
    chan: String, name: String
  ): Option[WatchHandler[_]] = for {
      handlers <- WidgetChannels.chanHandlers.get(chan)
      handler  <- handlers.get(name)
  } yield handler
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
  def watch(variable: String, handler: WatchHandler[_]): Unit =
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
  private[widgets] var chanHandlers: Map[String, Map[String, WatchHandler[_]]] =
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
     chan: String, variable: String, handler: WatchHandler[_]
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
