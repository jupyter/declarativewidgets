package declarativewidgets.util

import declarativewidgets.Comm
import org.apache.toree.comm.CommWriter
import org.apache.toree.utils.LogLike
import play.api.libs.json.{Json, JsValue}

/**
 * Provides functions for sending messages using a CommWriter.
 */
trait MessageSupport extends LogLike {

  /**
   * Send a state update for the given state key and value.
   * @param comm CommWriter to use for communication.
   * @param key Name of the state parameter to update.
   * @param value JSON representation of the new parameter value.
   */
  def sendState(comm: CommWriter, key: String, value: JsValue): Unit = {
    val msg = Json.obj(
      Comm.KeyMethod -> Comm.MethodUpdate,
      Comm.KeyState -> Map(
        key -> value
      )
    )
    logger.debug(s"Sending state message ${msg}")
    send(comm, msg)
  }

  /**
   * Send a status update with the given status and message.
   * @param comm CommWriter to use for communication.
   * @param status "ok" or "error"
   * @param msg Optional status message, e.g. an error message.
   */
  def sendStatus(comm: CommWriter, status: String, msg: String = ""): Unit = {
    val statusJson = Json.obj(
      Comm.KeyStatus -> status,
      Comm.KeyMessage -> msg,
      Comm.KeyTimestamp -> timestamp
    )
    sendState(comm, Comm.KeyStatusMsg, statusJson)
  }

  /**
   * Send a JSON message using the given comm writer.
   * @param comm CommWriter to use for communication.
   * @param msg Message to send.
   */
  def send(comm: CommWriter, msg: JsValue): Unit = comm.writeMsg(msg)

  private[util] def timestamp: Long = System.currentTimeMillis()

}
