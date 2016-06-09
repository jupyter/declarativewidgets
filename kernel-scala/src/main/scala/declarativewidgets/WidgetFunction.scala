/**
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */

package declarativewidgets

import declarativewidgets.util.{SerializationSupport, StandardFunctionSupport}
import org.apache.toree.comm.CommWriter
import org.apache.toree.kernel.protocol.v5.MsgData
import play.api.libs.json.{JsString, JsValue, Json}
import urth.widgets.util.SerializationSupport

import scala.util.{Failure, Success, Try}

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
        invokeFunc(name, args, limit) match {
          case Success(result) =>
            sendResult(result)
            sendOk()
          case Failure(t) =>
            sendError(s"Error invoking ${theFunctionName}: ${t.getCause}")
            logger.error(s"Error invoking ${theFunctionName}: ${t.getCause}")
        }
      case None =>
        sendError(s"No arguments were provided in message $msg for invocation!")
        logger.warn(s"No arguments were provided for invocation!")
    }
  }

  private[widgets] def invokeFunc(funcName: String, args: JsValue, limit: Int = limit): Try[JsValue] = {
    logger.debug(s"Invoking registered function with args ${args}")
    argMap(args) match {
      case Some(map) => invokeFunction(funcName, map) map (serialize(_, limit))
      case None => throw new RuntimeException(
          s"Invalid arguments $args for function $funcName"
      )
    }
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
    sendSignature(funcName) match {
      case Right(_)  => sendOk()
      case Left(msg) => sendError(msg)
    }
  }

  private[widgets] def registerLimit(limit: Int): Unit = {
    this.limit = limit
    logger.debug(s"Registered limit ${limit}.")
  }

  private[widgets] def sendSignature(funcName: String): Either[String, Unit] = {
    signature(funcName) match {
      case Some(sig) =>
        val sigJSON = Json.toJson(sig)
        logger.trace(s"Signature for ${funcName}: ${sigJSON}")
        Right(sendState(Comm.StateSignature, sigJSON))
      case None =>
        logger.trace(s"Could not determine signature for function $funcName.")
        Left(s"Invalid function name $funcName. Could not determine signature.")
    }
  }

  private[widgets] def sendResult(result: JsValue): Unit =
    sendState(Comm.StateResult, result)

}