package declarativewidgets

import declarativewidgets.util.{SerializationSupport, StandardFunctionSupport, MessageSupport}
import org.apache.toree.comm.CommWriter
import org.apache.toree.kernel.protocol.v5.MsgData
import org.apache.toree.utils.LogLike
import play.api.libs.json.{JsNull, JsValue}
import declarativewidgets.util.MessageSupport

/**
 * A widget that provides an API for setting bound channel variables.
 * @param comm CommWriter used for communication with the front-end.
 */
class WidgetChannels(val comm: CommWriter)
  extends Widget(comm) with StandardFunctionSupport with SerializationSupport {

  WidgetChannels.register(this)

  override def handleBackbone(msg: MsgData, msgSupport:MessageSupport): Unit = ()

  override def handleRequestState(msgContent: MsgData, msgSupport:MessageSupport): Unit ={
    val cachedData = WidgetChannels.cachedChannelData.flatMap {
      case (chan, data) =>
        data.map{
          case (key, value ) => WidgetChannels.chanKey(chan,key) -> serialize(value._1, value._2)
        }
    }.toMap

    msgSupport.sendState(cachedData)

    //once the request state is handled, the cache is cleared
    WidgetChannels.cachedChannelData.clear()
  }

  /**
   * Handles a Comm Message whose method is custom.
   * @param msgContent The content field of the Comm Message.
   */
  override def handleCustom(msgContent: MsgData, msgSupport:MessageSupport): Unit = {
    logger.debug(s"Handling custom message ${msgContent}...")
    (msgContent \ Comm.KeyEvent).asOpt[String] match {
      case Some(Comm.EventChange) => handleChange(msgContent) match {
        case Right(u)  => msgSupport.sendOk()
        case Left(msg) => msgSupport.sendError(msg)
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
  private[declarativewidgets] def handleChange(msgContent: MsgData): Either[String, Unit] =
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
  private[declarativewidgets] def parseMessage(
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
  private[declarativewidgets] def parseOldVal(msgContent: MsgData): Option[JsValue] = {
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
  private[declarativewidgets] def getHandler(
    chan: String, name: String
  ): Option[WatchHandler[_]] = for {
      handlers <- WidgetChannels.chanHandlers.get(chan)
      handler  <- handlers.get(name)
  } yield handler
}

/**
 * Common trait for what is returned by WidgetChannels.channel()
 */
trait Channel {
  val chan:String

  /**
   * Set a property on this channel using the given key and value.
   * @param key
   * @param value
   * @param limit Bounds the output for some serializers, e.g. limits the
   *              number of rows returned for a DataFrame.
   */
  def set(key: String, value: Any, limit: Int = Default.Limit): Unit

  /**
   * Watch the given variable on this channel for changes, and execute the
   * given handler when a change occurs.
   * @param variable Name of the variable to watch.
   * @param handler Handler to execute when a change to `variable` occurs.
   */
  def watch(variable: String, handler: WatchHandler[_]): Unit = WidgetChannels.watch(this.chan, variable, handler)
}

/**
 * Provides methods for interacting with a single channel.
 * @param comm CommWriter used for communication with the front-end.
 * @param chan Name of the channel.
 */
case class ConnectedChannel(comm: CommWriter, override val chan: String)
  extends Channel with SerializationSupport {

  val msgSupport:MessageSupport = Widget.toMessageSupport(comm)

  override def set(key: String, value: Any, limit: Int = Default.Limit): Unit =
    msgSupport.sendState(WidgetChannels.chanKey(chan, key), serialize(value, limit))
}

case class DisconnectedChannel(val cache: collection.mutable.Map[String, collection.mutable.Map[String,(Any,Int)]],
                               override val chan: String) extends Channel {

  override def set(key: String, value: Any, limit: Int): Unit = {
    val cachedData = cache.getOrElseUpdate(chan, collection.mutable.Map())
    cachedData(key) = (value, limit)
  }
}

/**
 * Provides access to Channel instances that use the most recently created
 * WidgetChannels' comm instance for communication.
 */
object WidgetChannels extends LogLike {

  // Stores the most recently created WidgetChannels widget instance.
  private[declarativewidgets] var theChannels: Option[WidgetChannels] = None

  // Maps channel name to a map of variable name to handler.
  private[declarativewidgets] var chanHandlers: Map[String, Map[String, WatchHandler[_]]] =
    Map()

  // Maps that caches channel data
  private[declarativewidgets] val cachedChannelData: collection.mutable.Map[String, collection.mutable.Map[String,(Any,Int)]] = collection.mutable.Map()

  /**
   * Provides a Channel instance for interacting with the channel
   * with the given name.
   * @param channelName Channel name.
   * @return Channel instance for `chan`.
   */
  def channel(channelName: String = Default.Channel): Channel = {
    this.theChannels.map{ c =>
      ConnectedChannel(c.comm, channelName)
    }.getOrElse{
      DisconnectedChannel(cachedChannelData, channelName)
    }
  }

  /**
   * Register the handler to execute when a change occurs to the given
   * variable name on the given channel.
   * @param chan Channel name.
   * @param variable Variable name to watch for changes.
   * @param handler Handler to register.
   */
  private[declarativewidgets] def watch(
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
  private[declarativewidgets] def register(widget: WidgetChannels) : Unit = {
    this.theChannels = Some(widget)
    logger.debug(s"Registered widget $widget as the global Channels.")
  }

  private[declarativewidgets] def chanKey(chan: String, key: String): String = s"$chan:$key"

}
