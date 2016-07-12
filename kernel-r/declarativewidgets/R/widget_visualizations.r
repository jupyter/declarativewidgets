#Widget Visualization one line displays

explore <- function(df) {
    IRdisplay::display_html(paste("<link rel='import' href='urth_components/urth-viz-vega/urth-viz-vega-explorer.html'
                                    is='urth-core-import' package='ibm-et/urth-viz-vega'>
                                    <urth-viz-vega-explorer multi-select ref='", df, "'></urth-viz-vega-explorer>", sep = ""))
}