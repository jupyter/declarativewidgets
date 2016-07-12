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
  def explore(df: String) = {
    val explorerImport =
      """
        <link rel='import' href='urth_components/urth-viz-vega/urth-viz-vega-explorer.html'
          is='urth-core-import' package='ibm-et/urth-viz-vega'>
      """
    getKernel.display.html(s"$explorerImport <urth-viz-vega-explorer multi-select ref='$df'></urth-viz-vega-explorer>")
  }
}