#Widget Visualization one line displays

explore <- function(df, channel='default', selection_var=NULL) {
    selection <- ifelse(is.null(selection_var), "", paste("selection={{", selection_var, "}}", sep=""))
    IRdisplay::display_html(paste("<link rel='import' href='urth_components/declarativewidgets-explorer/urth-viz-explorer.html'
                                    is='urth-core-import' package='jupyter-incubator/declarativewidgets_explorer'>
                                    <template is='urth-core-bind' channel='", channel, "'>
                                        <urth-viz-explorer ref='", df, "' ", selection, "></urth-viz-explorer>",
                                    "</template>", sep = ""))
}