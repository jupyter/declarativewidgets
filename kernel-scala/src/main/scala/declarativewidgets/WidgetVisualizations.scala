/**
  * Copyright (c) Jupyter Development Team.
  * Distributed under the terms of the Modified BSD License.
  */

package declarativewidgets

/**
  * Object that contains Widget Visualization one line displays
  */
object WidgetVisualizations {
  /**
    * Renders the urth-viz-explorer widget to the user output
    *
    * @param df Dataframe variable name to render/explore
    */
  def explore(df: String, channel: String = "default", selectionVar: String = null) = {
    val selection = if(selectionVar == null) "" else s"selection={{$selectionVar}}"
    val explorerImport =
      """
        <link rel='import' href='urth_components/declarativewidgets-explorer/urth-viz-explorer.html'
          is='urth-core-import' package='jupyter-incubator/declarativewidgets_explorer'>
      """
    getKernel.display.html(s"$explorerImport " +
                           s"<template is='urth-core-bind' channel='$channel'>" +
                              s"<urth-viz-explorer ref='$df' $selection></urth-viz-explorer>" +
                            "</template>")
  }
}