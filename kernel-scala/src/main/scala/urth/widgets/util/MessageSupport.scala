package urth.widgets.util

import com.ibm.spark.comm.CommWriter
import com.ibm.spark.utils.LogLike
import play.api.libs.json.{Json, JsValue}
import urth.widgets.Comm

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
   * Send a JSON message using the given comm writer.
   * @param comm CommWriter to use for communication.
   * @param msg Message to send.
   */
  def send(comm: CommWriter, msg: JsValue): Unit = comm.writeMsg(msg)

}
