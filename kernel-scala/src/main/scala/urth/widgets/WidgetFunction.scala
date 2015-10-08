/**
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */

package urth.widgets

import com.ibm.spark.comm.CommWriter
import com.ibm.spark.kernel.protocol.v5.MsgData
import play.api.libs.json.{JsString, JsValue, Json}
import urth.widgets.util.{SerializationSupport, StandardFunctionSupport}

/**
 * A widget for invoking a function in the kernel.
 *
 * @param comm CommWriter used for communication with the front-end.
 */
class WidgetFunction(comm: CommWriter)
  extends Widget(comm) with StandardFunctionSupport with SerializationSupport {

  // name of the function in the kernel
  var theFunctionName: String = _

  // limit for serialized output, e.g. a limit on DataFrame rows
  var limit: Int = 100

  /**
   * Handles a Comm Message whose method is backbone.
   * @param msg The Comm Message.
   */
  def handleBackbone(msg: MsgData): Unit = {
    logger.debug(s"Handling backbone message ${msg}...")
    (msg \ Comm.KeySyncData \ Comm.KeyFunctionName).asOpt[String] match {
      case Some(name) =>
        logger.trace(s"Sync data ${Comm.KeyFunctionName}: $name")
        val funcName = name.toString
        registerFunction(funcName)
        sendSignature(funcName)
      case _ => logger.error(s"No ${Comm.KeyFunctionName} value provided!")
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
      case Some(Comm.EventInvoke) =>
        handleInvoke(msgContent, this.theFunctionName, this.limit)
      case Some(Comm.EventSync) =>
        sendSignature(this.theFunctionName)
      case Some(evt) =>
        logger.warn(s"Unhandled custom event ${evt}.")
      case None =>
        logger.warn("No event value in custom comm message.")
    }
  }

  private[widgets] def handleInvoke(msg: MsgData, name: String, limit: Int): Unit = {
    logger.debug(s"Handling invoke message ${msg}...")
    (msg \ Comm.KeyArgs).asOpt[JsValue] match {
      case Some(args) =>
        invokeFunc(name, args).map(serialize(_, limit)) match {
          case Some(result) => sendResult(result)
          case None => logger.error(s"No result for ${theFunctionName} invoke.")
        }
      case None => logger.warn(s"No arguments were provided for invocation!")
    }
  }

  private[widgets] def invokeFunc(funcName: String, args: JsValue): Option[Any] = {
    logger.debug(s"Invoking registered function with args ${args}")
    val result = for {
      map <- argMap(args)
      res <- invokeFunction(funcName, map)
    } yield res
    logger.debug(s"Function invocation result: ${result}")
    result
  }

  private def argMap(args: JsValue): Option[Map[String, String]] =
    args.asOpt[Map[String, JsValue]] match {
      case Some(map) => Some(map.map(kv => kv._2 match {
        case JsString(str) => (kv._1, str)
        case str => (kv._1, str.toString)
      }))
      case None =>
        logger.error(s"Error converting JSON ${args} to a map.")
        None
    }

  private[widgets] def registerFunction(funcName: String): Unit = {
    this.theFunctionName = funcName
    logger.debug(s"Registered function ${funcName}.")
  }

  private[widgets] def registerLimit(limit: Int): Unit = {
    this.limit = limit
    logger.debug(s"Registered limit ${limit}.")
  }

  private[widgets] def sendSignature(funcName: String): Unit = {
    signature(funcName) match {
      case Some(sig) =>
        val sigJSON = Json.toJson(sig)
        logger.trace(s"Signature for ${funcName}: ${sigJSON}")
        sendState(Comm.StateSignature, sigJSON)
      case None => logger.warn(s"Could not determine signature for ${funcName}")
    }
  }

  private[widgets] def sendResult(result: JsValue): Unit =
    sendState(Comm.StateResult, result)

}