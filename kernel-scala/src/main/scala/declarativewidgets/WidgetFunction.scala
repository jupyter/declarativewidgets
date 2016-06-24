/**
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */

package declarativewidgets

import declarativewidgets.util.{MessageSupport, SerializationSupport, StandardFunctionSupport}
import org.apache.toree.comm.CommWriter
import org.apache.toree.kernel.protocol.v5.MsgData
import play.api.libs.json.{JsString, JsValue, Json}

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
  override def handleBackbone(msg: MsgData, msgSupport:MessageSupport): Unit = {
    logger.debug(s"Handling backbone message ${msg}...")
    (msg \ Comm.KeySyncData \ Comm.KeyFunctionName).asOpt[String] match {
      case Some(name) =>
        logger.trace(s"Sync data ${Comm.KeyFunctionName}: $name")
        val funcName = name.toString
        registerFunction(funcName, msgSupport)
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
  override def handleCustom(msgContent: MsgData, msgSupport:MessageSupport): Unit = {
    logger.debug(s"Handling custom message ${msgContent}...")
    (msgContent \ Comm.KeyEvent).asOpt[String] match {
      case Some(Comm.EventInvoke) =>
        handleInvoke(msgContent, this.theFunctionName, this.limit, msgSupport)
      case Some(Comm.EventSync) =>
        sendSignature(this.theFunctionName, msgSupport)
      case Some(evt) =>
        logger.warn(s"Unhandled custom event ${evt}.")
      case None =>
        logger.warn("No event value in custom comm message.")
    }
  }

  private[declarativewidgets] def handleInvoke(msg: MsgData, name: String, limit: Int, msgSupport:MessageSupport): Unit = {
    logger.debug(s"Handling invoke message ${msg}...")
    (msg \ Comm.KeyArgs).asOpt[JsValue] match {
      case Some(args) =>
        invokeFunc(name, args, limit) match {
          case Success(result) =>
            sendResult(result, msgSupport)
            msgSupport.sendOk()
          case Failure(t) =>
            msgSupport.sendError(s"Error invoking ${theFunctionName}: ${t.getCause}")
            logger.error(s"Error invoking ${theFunctionName}: ${t.getCause}")
        }
      case None =>
        msgSupport.sendError(s"No arguments were provided in message $msg for invocation!")
        logger.warn(s"No arguments were provided for invocation!")
    }
  }

  private[declarativewidgets] def invokeFunc(funcName: String, args: JsValue, limit: Int = limit): Try[JsValue] = {
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

  private[declarativewidgets] def registerFunction(funcName: String, msgSupport:MessageSupport): Unit = {
    this.theFunctionName = funcName
    logger.debug(s"Registered function ${funcName}.")
    sendSignature(funcName, msgSupport) match {
      case Right(_)  => msgSupport.sendOk()
      case Left(msg) => msgSupport.sendError(msg)
    }
  }

  private[declarativewidgets] def registerLimit(limit: Int): Unit = {
    this.limit = limit
    logger.debug(s"Registered limit ${limit}.")
  }

  private[declarativewidgets] def sendSignature(funcName: String, msgSupport:MessageSupport): Either[String, Unit] = {
    signature(funcName) match {
      case Some(sig) =>
        val sigJSON = Json.toJson(sig)
        logger.trace(s"Signature for ${funcName}: ${sigJSON}")
        Right(msgSupport.sendState(Comm.StateSignature, sigJSON))
      case None =>
        logger.trace(s"Could not determine signature for function $funcName.")
        Left(s"Invalid function name $funcName. Could not determine signature.")
    }
  }

  private[declarativewidgets] def sendResult(result: JsValue, msgSupport:MessageSupport): Unit =
    msgSupport.sendState(Comm.StateResult, result)

}