/**
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */

import org.apache.toree.kernel.api.Kernel
import org.apache.spark.repl.SparkIMain

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

    // JavaScript code to load the declarative widgets extension.
    // Code sent to the front end from here may be executed after
    // extension initialization (iterative cell execution) or
    // before (Run All, Reload). This code works together with the
    // extension initialization to ensure that the required API is
    // available in all scenarios.
    //
    // Urth._initialized is a deferred that is resolved by the extension
    // initialization after the global Urth instance has been setup.
    // If extension initialization has not completed a new deferred is
    // initialized which extension initialization will resolve.
    //
    // Urth.whenReady is a public API defined by extension initialization
    // to delay javascript execution until dependencies have loaded. If
    // extension initialization has not completed a wrapper implementation
    // is setup which will invoke the real implementation when it is available.
    val code = """
      window.Urth = window.Urth || {};
      Urth._initialized = Urth._initialized || $.Deferred();
      Urth.whenReady = Urth.whenReady || function(cb) {
        Urth._initialized.then(function() {
          Urth.whenReady(cb);
        });
      };
      Urth.whenReady(function() { console.log("Declarative widgets connected.") });"""

    kernel.display.javascript(code)
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
}
