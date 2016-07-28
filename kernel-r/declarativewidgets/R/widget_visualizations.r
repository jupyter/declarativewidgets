# Widget Visualization one line displays

#' Explore function
#'
#' The explore function is a wrapper around urth-viz-explorer.
#' See the jupyter-incubator/declarativewidgets_explorer project.
#'
#' @name explore
#' @param df  the dataframe itself or the string representation of the variable
#' @param channel  The channel to bind to defaulted to default
#' @param selection_var  The selection variable by default not used/applied
explore <- function(df, channel='default', selection_var=NULL) {
    unique_df_name <- "the_literal_template_df_name"
    register_explore_df <- function(df) {
        #assigns df to global env and returns a unique name
        assign(unique_df_name, df, envir = .GlobalEnv)
        return (unique_df_name)
    }

    exlore_df_name <- ifelse(class(df) == "data.frame", register_explore_df(df), df)
    selection <- ifelse(is.null(selection_var), "", paste("selection={{", selection_var, "}}", sep=""))

    IRdisplay::display_html(paste("<link rel='import' href='urth_components/declarativewidgets-explorer/urth-viz-explorer.html'
                                    is='urth-core-import' package='jupyter-incubator/declarativewidgets_explorer'>
                                    <template is='urth-core-bind' channel='", channel, "'>
                                        <urth-viz-explorer ref='", exlore_df_name, "' ", selection, "></urth-viz-explorer>",
                                    "</template>", sep = ""))
}
