/**
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */

package urth

import com.ibm.spark.kernel.api.Kernel
import org.apache.spark.repl.SparkIMain

package object widgets {

  // Types
  type WatchHandler = (_, _) => Unit

  object WidgetClass {
    val Function  = "urth.widgets.widget_function.Function"
    val DataFrame = "urth.widgets.widget_dataframe.DataFrame"
    val Channels  = "urth.widgets.widget_channels.Channels"
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

    // Method types
    val MethodBackbone   = "backbone"
    val MethodCustom     = "custom"
    val MethodUpdate     = "update"

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
  }

  object Default {
    val Limit: Int = 100
    val Channel: String = "default"
  }

  private var _the_kernel: Kernel = _

  def initWidgets(implicit kernel: Kernel): Unit = {
      val registrar = kernel.comm.register("ipython.widget")
      registrar.addOpenHandler(Widget.openCallback)
      registrar.addMsgHandler(Widget.msgCallback)
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
}
