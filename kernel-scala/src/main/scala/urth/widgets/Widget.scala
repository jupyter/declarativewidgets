/**
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */

package urth.widgets

import com.ibm.spark.comm.CommWriter
import com.ibm.spark.kernel.protocol.v5.{MsgData, UUID}
import com.ibm.spark.utils.LogLike
import play.api.libs.json.{Json, JsValue}
import urth.widgets.exceptions.WidgetNotAvailableException
import urth.widgets.util.MessageSupport

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
  private[widgets] def createWidgetInstance(
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
  val msgCallback = (w: CommWriter, id: UUID, msg: MsgData) => {
    logger.trace(s"Widget received a comm message $msg")
    widgets(id).handleMsg(msg)
  }

}

/**
 * Represents a Widget that can handle Comm messages.
 * @param comm Comm used to communicate with the front-end.
 */
abstract class Widget(comm: CommWriter) extends LogLike with MessageSupport {

  logger.debug(s"Widget ${getClass.getSimpleName} initialized.")

  /**
   * Handle a Comm Message from the front-end by routing it to a
   * handler based on the method.
   * @param msg The Comm Message.
   */
  def handleMsg(msg: MsgData) = {
    logger.debug(s"Widget handling message ${msg}")

    (msg \ Comm.KeyMethod).asOpt[String] match {
      case Some(Comm.MethodBackbone) => handleBackbone(msg)
      case Some(Comm.MethodCustom) => (msg \ Comm.KeyContent).asOpt[JsValue] match {
        case Some(contentJSON) => handleCustom(contentJSON)
        case None => logger.warn(
          s"No ${Comm.KeyContent} key in custom message $msg. Ignoring message."
        )
      }
      case m => logger.warn(s"Unhandled ${Comm.KeyMethod} value ${m}")
    }
  }

  /**
   * Send a state update to the front-end for the given state key and value.
   * @param key Name of the state parameter to update.
   * @param value JSON representation of the new parameter value.
   */
  def sendState(key: String, value: JsValue): Unit = sendState(comm, key, value)

  /**
   * Handles a Comm Message whose method is backbone.
   * @param msg The Comm Message.
   */
  def handleBackbone(msg: MsgData)

  /**
   * Handles a Comm Message whose method is custom.
   * @param msgContent The content field of the Comm Message.
   */
  def handleCustom(msgContent: MsgData)

}