package declarativewidgets.util

import declarativewidgets.Comm
import org.apache.toree.comm.CommWriter
import org.apache.toree.utils.LogLike
import play.api.libs.json.{JsValue, Json}

/**
 * Provides functions for sending messages using a CommWriter.
 */
case class MessageSupport(val comm:CommWriter) extends LogLike {

  /**
   * Send a state update for the given state key and value.
   * @param key Name of the state parameter to update.
   * @param value JSON representation of the new parameter value.
   */
  def sendState(key: String, value: JsValue): Unit = {
    sendState(Map(key -> value))
  }

  /**
   * Send a state update with the give Map of key/value pairs.
   * @param state A Map with name of Name of the state parameter and JSON value to update.
   */
  def sendState(state: Map[String,JsValue]): Unit = {

    val msg = Json.obj(
      Comm.KeyMethod -> Comm.MethodUpdate,
      Comm.KeyState -> state
    )
    logger.debug(s"Sending state message ${msg}")
    send(msg)
  }

  /**
   * Send a status update with the given status and message.
   * @param status "ok" or "error"
   * @param msg Optional status message, e.g. an error message.
   */
  def sendStatus(status: String, msg: String = ""): Unit = {
    val statusJson = Json.obj(
      Comm.KeyStatus -> status,
      Comm.KeyMessage -> msg,
      Comm.KeyTimestamp -> timestamp
    )
    sendState(Comm.KeyStatusMsg, statusJson)
  }

  /**
   * Send a status error message to the front-end with the given message.
   * @param msg An error message.
   */
  def sendError(msg: String) = sendStatus(Comm.StatusError, msg)

  /**
   * Send a status ok message to the front-end with the given message.
   * @param msg An optional status message.
   */
  def sendOk(msg: String = "") = sendStatus(Comm.StatusOk, msg)

  /**
   * Send a JSON message using the given comm writer.
   * @param msg Message to send.
   */
  def send(msg: JsValue): Unit = comm.writeMsg(msg)

  private[util] def timestamp: Long = System.currentTimeMillis()

}
