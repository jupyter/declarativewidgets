/**
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */

import org.apache.spark.repl.SparkIMain
import org.apache.toree.kernel.api.Kernel

package object declarativewidgets {

  // Types
  type WatchHandler[T] = (Option[T], T) => Unit

  object WidgetClass {
    val Function  = "declarativewidgets.Function"
    val DataFrame = "declarativewidgets.DataFrame"
    val Channels  = "declarativewidgets.Channels"
  }

  object Comm {
    // JSON keys
    val KeyMethod        = "method"


    val KeyWidgetClass   = "widget_class"
    val KeySyncData      = "sync_data"
    val KeyFunctionName  = "function_name"
    val KeyDataFrameName = "variable_name"
    val KeyLimit         = "limit"
    val KeyEvent         = "event"
    val KeyArgs          = "args"
    val KeyState         = "state"
    val KeyContent       = "content"
    val KeyType          = "type"
    val KeyRequired      = "required"
    val KeyDefaultValue  = "value"
    val KeyStatus        = "status"
    val KeyStatusMsg     = "__status__"
    val KeyMessage       = "msg"
    val KeyTimestamp     = "timestamp"

    // Method types
    val MethodBackbone      = "backbone"
    val MethodRequestState  = "request_state"
    val MethodCustom        = "custom"
    val MethodUpdate        = "update"

    // Event types
    val EventChange      = "change"
    val EventInvoke      = "invoke"
    val EventSync        = "sync"

    // State types
    val StateResult      = "result"
    val StateSignature   = "signature"
    val StateValue       = "value"

    // Change types
    val ChangeData       = "data"
    val ChangeChannel    = "channel"
    val ChangeName       = "name"
    val ChangeOldVal     = "old_val"
    val ChangeNewVal     = "new_val"

    // Status types
    val StatusError      = "error"
    val StatusOk         = "ok"
  }

  object SymbolKind {
    val Method = "method"
    val Value = "value"
  }

  object Default {
    val Limit: Int = 100
    val Channel: String = "default"
  }

  private[declarativewidgets] var _the_kernel: Kernel = _

  def initWidgets(implicit kernel: Kernel): Unit = {
    /*
     * Support for ipywidget 4.x Client
     */
    kernel.comm.register("ipython.widget")
      .addOpenHandler(Widget.openCallback)
      .addMsgHandler(Widget.msgCallback)

    /*
     * Support for ipywidget 5.x Client
     */
    kernel.comm.register("jupyter.widget")
      .addOpenHandler(Widget.openCallback)
      .addMsgHandler(Widget.msgCallback)
    _the_kernel = kernel
  }

  def getKernel: Kernel = {
    _the_kernel
  }

  def sparkIMain: SparkIMain = {
    val sparkIMainMethod = getKernel.interpreter.getClass.getMethod("sparkIMain")
    val sparkIMain = sparkIMainMethod.invoke(getKernel.interpreter).asInstanceOf[org.apache.spark.repl.SparkIMain]
    sparkIMain
  }

  def channel(chan: String = Default.Channel): Channel =
    WidgetChannels.channel(chan)

  def explore(df: Any, channel: String = "default", properties: Map[String, Any] = Map(), bindings: Map[String, String] = Map()): Unit = {
    util.Explore.explore(df, channel, properties, bindings)
  }
}
