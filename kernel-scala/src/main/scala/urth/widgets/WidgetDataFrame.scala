/**
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */

package urth.widgets

import com.ibm.spark.comm.CommWriter
import com.ibm.spark.interpreter.Interpreter
import com.ibm.spark.kernel.protocol.v5.MsgData
import play.api.libs.json.JsValue
import urth.widgets.util.SerializationSupport

/**
 * A widget for retrieving the value of a DataFrame in the kernel.
 *
 * @param comm CommWriter used for communication with the front-end.
 */
class WidgetDataFrame(comm: CommWriter)
  extends Widget(comm) with SerializationSupport {

  // name of the DataFrame in the kernel
  var variableName: String = _

  // maximum number of rows to send
  var limit: Int = Default.Limit

  lazy val kernelInterpreter: Interpreter = getKernel.interpreter

  /**
   * Handles a Comm Message whose method is backbone.
   * @param msg The Comm Message.
   */
  def handleBackbone(msg: MsgData): Unit = {
    logger.debug(s"Handling backbone message ${msg}...")
    (msg \ Comm.KeySyncData \ Comm.KeyDataFrameName).asOpt[String] match {
      case Some(dfName) => registerName(dfName)
      case _ => logger.error(s"No ${Comm.KeyDataFrameName} value provided!")
    }
    (msg \ Comm.KeySyncData \ Comm.KeyLimit).asOpt[Int] match {
      case Some(lim) => registerLimit(lim)
      case _ => logger.warn(
        s"No ${Comm.KeyLimit} value provided. Using limit = ${this.limit}.")
    }
  }

  /**
   * Handles a Comm Message whose method is custom.
   * @param msgContent The content field of the Comm Message.
   */
  def handleCustom(msgContent: MsgData): Unit = {
    logger.debug(s"Handling custom message ${msgContent}...")
    (msgContent \ Comm.KeyEvent).asOpt[String] match {
      case Some(Comm.EventSync) =>
        serializeAndSend(this.variableName, this.limit)
      case Some(evt) =>
        logger.warn(s"Unhandled custom event ${evt}.")
      case None =>
        logger.warn("No event value in custom comm message.")
    }
  }

  private[widgets] def registerLimit(limit: Int): Unit = {
    this.limit = limit
    logger.debug(s"Registered limit ${limit}.")
  }

  private[widgets] def registerName(name: String): Unit = {
    this.variableName = name
    logger.debug(s"Registered DataFrame variable name ${name}.")
    serializeAndSend(name, this.limit) match {
      case Right(_)  => sendOk()
      case Left(msg) => sendError(msg)
    }
  }

  private[widgets] def serializeAndSend(
    name: String, limit: Int
  ): Either[String, Unit] = {
    kernelInterpreter.read(name) match {
      case Some(df) =>
        val serialized = serialize(df, limit)
        sendSyncData(serialized)
        logger.trace(s"Sent sync message for DataFrame $name")
        Right()
      case None =>
        logger.warn(s"DataFrame ${name} not found! No sync message sent.")
        Left(s"DataFrame ${name} not found!")
    }
  }

  private[widgets] def sendSyncData(result: JsValue) =
    sendState(Comm.StateValue, result)

}
