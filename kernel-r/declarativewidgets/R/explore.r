# Widget Visualization one line displays

# explore_env that houses the explore_id
explore_env <- new.env()
#' gets a unique explore id by incrementing an id
#'
#' The function returns a unique id to be used for the explore global df references
#' The explore_id is protected within the explore_env
#'
#' @name get_unique_explore_id
get_unique_explore_id <- function() {
    explore_id <- "explore_id"
    #init if doesn't exist
    if(!exists(explore_id, envir = explore_env)) {
        assign(explore_id, 0, envir = explore_env)
    }
    #get and increment by one
    temp_explore_id <- get(explore_id, envir  = explore_env)
    assign(explore_id, temp_explore_id+1, envir = explore_env)
    #return the id after it was incremented
    return (get(explore_id, envir  = explore_env))
}

stringify_properties <- function(properties) {
    properties_str <- ""
    for (i in names(properties)) {
        prop <- {
                if (class(properties[[i]]) == "logical") {
                    if (properties[[i]]) {
                        i
                    }
            } else {
                paste(i, "=", as.character(properties[[i]]), sep="")
            }
        }
        properties_str <- paste(properties_str, prop)
    }
    return (properties_str)
}

stringify_bindings <- function(bindings) {
    bindings_str <- ""
    for (i in names(bindings)) {
        binding <- paste(i, "={{", bindings[[i]], "}}", sep="")
        bindings_str <- paste(bindings_str, binding)
    }
    return (bindings_str)
}


#' Explore function
#'
#' The explore function is a wrapper around urth-viz-explorer.
#' See the jupyter-incubator/declarativewidgets_explorer project.
#'
#' @name explore
#' @param df  the dataframe itself or the string representation of the variable
#' @param channel  The channel to bind to defaulted to default
#' @param properties  The properties e.g. properties <- list("selection-as-object"=FALSE, foo=5)
#' @param bindings  The bindings e.g. bindings <- list(selection='sel')
explore <- function(df, channel='default', properties=list(), bindings=list()) {
    unique_df_name <- paste("the_literal_template_df_name_", get_unique_explore_id(), sep = "")
    register_explore_df <- function(df) {
        #assigns df to global env and returns a unique name
        assign(unique_df_name, df, envir = .GlobalEnv)
        return (unique_df_name)
    }

    exlore_df_name <- ifelse(class(df) == "data.frame" || class(df) == "DataFrame", register_explore_df(df), df)

    IRdisplay::display_html(paste("<link rel='import' href='urth_components/declarativewidgets-explorer/urth-viz-explorer.html'
                                    is='urth-core-import' package='jupyter-incubator/declarativewidgets_explorer'>
                                    <template is='urth-core-bind' channel='", channel, "'>
                                        <urth-viz-explorer ref='", exlore_df_name, "'",
                                    stringify_properties(properties), stringify_bindings(bindings), "></urth-viz-explorer>",
                                    "</template>", sep = ""))
}
