#Widget Visualization one line displays

explore <- function(df) {
    IRdisplay::display_html(paste("<link rel='import' href='urth_components/declarativewidgets-explorer/urth-viz-explorer.html'
                                    is='urth-core-import' package='jupyter-incubator/declarativewidgets_explorer'>
                                    <urth-viz-explorer ref='", df, "'></urth-viz-explorer>", sep = ""))
}