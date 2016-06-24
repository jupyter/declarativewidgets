/**
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */

package declarativewidgets

import declarativewidgets.query.QuerySupport
import declarativewidgets.util.{MessageSupport, SerializationSupport}
import org.apache.spark.sql.DataFrame
import org.apache.toree.comm.CommWriter
import org.apache.toree.interpreter.Interpreter
import org.apache.toree.kernel.protocol.v5.MsgData
import play.api.libs.json._

/**
 * A widget for retrieving the value of a DataFrame in the kernel.
 *
 * @param comm CommWriter used for communication with the front-end.
 */
class WidgetDataFrame(comm: CommWriter)
  extends Widget(comm) with SerializationSupport with QuerySupport{

  // name of the DataFrame in the kernel
  var variableName: String = _

  // maximum number of rows to send
  var limit: Int = Default.Limit

  // the query to apply to the DataFrame. Defaults to JSON empty array.
  var query: String = "[]"

  lazy val kernelInterpreter: Interpreter = getKernel.interpreter

  /**
   * Handles a Comm Message whose method is backbone.
   * @param msg The Comm Message.
   */
  override def handleBackbone(msg: MsgData, msgSupport:MessageSupport): Unit = {
    logger.debug(s"Handling backbone message ${msg}...")
    (msg \ Comm.KeySyncData \ Comm.KeyLimit).asOpt[Int].foreach(registerLimit(_))

    (msg \ Comm.KeySyncData \ Comm.KeyDataFrameName).asOpt[String].foreach(registerName(_))

    (msg \ Comm.KeySyncData \ "query").asOpt[String].foreach(registerQuery(_))
  }

  /**
   * Handles a Comm Message whose method is custom.
   * @param msgContent The content field of the Comm Message.
   */
  override def handleCustom(msgContent: MsgData, msgSupport:MessageSupport): Unit = {
    logger.debug(s"Handling custom message ${msgContent}...")
    (msgContent \ Comm.KeyEvent).asOpt[String] match {
      case Some(Comm.EventSync) =>
        syncData(msgSupport)
      case Some(evt) =>
        logger.warn(s"Unhandled custom event ${evt}.")
      case None =>
        logger.warn("No event value in custom comm message.")
    }
  }

  private[declarativewidgets] def registerLimit(limit: Int): Unit = {
    this.limit = limit
    logger.debug(s"Registered limit ${limit}.")

  }

  private[declarativewidgets] def registerName(name: String): Unit = {
    this.variableName = name
    logger.debug(s"Registered DataFrame variable name ${name}.")
  }

  private[declarativewidgets] def registerQuery(query: String): Unit = {
    this.query = query
    logger.debug(s"Registered query ${query}.")
  }

  private[declarativewidgets] def theDataframe = {
    kernelInterpreter.read(variableName)
  }

  private[declarativewidgets] def syncData(msgSupport:MessageSupport) = {

    val result: Either[String, JsValue] = theDataframe match {
      case Some(df:DataFrame) =>
         try{
           val query = Json.parse(this.query).asOpt[JsArray].getOrElse(new JsArray())
           Right(serialize(applyQuery(df, query), this.limit))
         }
        catch {
          case parseError:com.fasterxml.jackson.core.JsonParseException =>
            parseError.printStackTrace()
            Left(parseError.getMessage)
        }

      case Some(_) =>
        logger.warn(s"${this.variableName} is not a DataFrame")
        Left(s"${this.variableName} is not a DataFrame")

      case None =>
        logger.warn(s"DataFrame ${this.variableName} not found! No sync message sent.")
        Left(s"DataFrame ${this.variableName} not found!")
    }

    result match {
      case Right(serDf) =>
        sendSyncData(serDf, msgSupport)
        msgSupport.sendOk()

      case Left(error) =>
        msgSupport.sendError(error)
    }
  }

  private[declarativewidgets] def sendSyncData(result: JsValue,msgSupport:MessageSupport) =
    msgSupport.sendState(Comm.StateValue, result)

}
