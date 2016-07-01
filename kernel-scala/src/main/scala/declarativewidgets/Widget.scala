/**
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */

package declarativewidgets

import declarativewidgets.exceptions.WidgetNotAvailableException
import declarativewidgets.util.MessageSupport
import org.apache.toree.comm.CommWriter
import org.apache.toree.kernel.protocol.v5.{MsgData, UUID}
import org.apache.toree.utils.LogLike
import play.api.libs.json.{Json, JsValue}

/**
 * Object that contains Comm handlers related to widgets, and manages
 * widget creation.
 */
object Widget extends LogLike {

  val widgets = collection.mutable.HashMap[UUID, Widget]()

  /**
   * Create a widget instance corresponding to the given class name, and
   * associated with the given comm instance.
   * @param klassName Widget class name sent from the front-end.
   * @param comm Comm instance to associate with the created Widget.
   * @return The new Widget.
   * @throws WidgetNotAvailableException given an invalid `klassName`
   */
  private[declarativewidgets] def createWidgetInstance(
    klassName: String, comm: CommWriter
  ): Widget = {
    logger.debug(s"Creating widget for class ${klassName}")
    klassName match {
      case WidgetClass.Function  => new WidgetFunction(comm)
      case WidgetClass.DataFrame => new WidgetDataFrame(comm)
      case WidgetClass.Channels  => new WidgetChannels(comm)
      case _ => throw new WidgetNotAvailableException(klassName)
    }
  }

  /**
   * A Comm Open callback that instantiates a Widget based on the
   * class name found in the message.
   */
  val openCallback = (w: CommWriter, id: UUID, target: String, msg: MsgData) => {
    logger.trace(s"Widget received comm open message $msg")
    (msg \ Comm.KeyWidgetClass).asOpt[String] match {
      case Some(klass) =>
        widgets += (id -> Widget.createWidgetInstance(klass.toString, w))
        ()
      case None =>
        logger.error(s"No widget class provided in comm open message!")
        ()
    }
  }

  /**
   * A Comm Msg callback that routes message handling to the proper Widget
   * based on the Comm ID.
   */
  val msgCallback = (commWriter: CommWriter, id: UUID, msg: MsgData) => {
    logger.trace(s"Widget received a comm message $msg")
    widgets(id).handleMsg(msg, commWriter)
  }

  def toMessageSupport(commWriter:CommWriter):MessageSupport = MessageSupport(commWriter)
}

/**
 * Represents a Widget that can handle Comm messages.
 * @param comm Comm used to communicate with the front-end.
 */
abstract class Widget(comm: CommWriter) extends LogLike {

  logger.debug(s"Widget ${getClass.getSimpleName} initialized.")

  /**
   * Handle a Comm Message from the front-end by routing it to a
   * handler based on the method.
   * @param msg The Comm Message.
   */
  def handleMsg(msg: MsgData, commWriter:CommWriter) = {
    logger.debug(s"Widget handling message ${msg}")

    val msgSupport = Widget.toMessageSupport(commWriter)

    (msg \ Comm.KeyMethod).asOpt[String] match {
      case Some(Comm.MethodBackbone) => handleBackbone(msg, msgSupport)
      case Some(Comm.MethodRequestState) => handleRequestState(msg, msgSupport)
      case Some(Comm.MethodCustom) => (msg \ Comm.KeyContent).asOpt[JsValue] match {
        case Some(contentJSON) => handleCustom(contentJSON, msgSupport)
        case None => logger.warn(
          s"No ${Comm.KeyContent} key in custom message $msg. Ignoring message."
        )
      }
      case m => logger.warn(s"Unhandled ${Comm.KeyMethod} value ${m}")
    }
  }

  /**
   * Handles a Comm Message whose method is backbone.
   * @param msg The Comm Message.
   */
  def handleBackbone(msg: MsgData, msgSupport:MessageSupport)

  /**
   * Handles a Comm Message whose method is custom.
   * @param msgContent The content field of the Comm Message.
   */
  def handleCustom(msgContent: MsgData, msgSupport:MessageSupport)

  /**
   * Handles a Comm Message whose method is request_state. Defaults to returning an empty state
   * @param msgContent The Comm message
   */
  def handleRequestState(msgContent: MsgData, msgSupport:MessageSupport)  = msgSupport.sendState(Map[String,JsValue]())

}