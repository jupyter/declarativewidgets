#' @include widget.r serializer.r
NULL

# Creates a protected environment for channels and related init cached related data to live!
widget_channels_env <- new.env()

#Access a variable that is cached in the widget_channels_env
get_from_widget_channel_env_cache <- function(variable) {
    if(!exists(variable, envir = widget_channels_env)) {
        assign(variable, list(), envir = widget_channels_env)
    }
    return (get(variable, envir  = widget_channels_env))
}

#Put (Create or Replace) a variable that is cached in the widget_channels_env
put_from_widget_channel_env_cache <- function(variable, value) {
    assign(variable, value, envir = widget_channels_env)
}

#' Widget_Channel
#'
#' Description = Encapsulates a widget_channel,
#                where a channel has data (a -> 7) and a handler (a, function to manipulate value)
Channel <- setRefClass(
    'Channel',
    fields = list(
        channel_id = 'character'
    ),
    methods = list(
        #helper to perform and action on a key/value pair (example set/watch)
        update_key_action = function(func, cached_data_var, key, value) {
            #If the Channels model hasn't been created yet, keep track of values
            #that have been specified, otherwise go ahead and perform the action for the value.
            if(exists("the_channels", envir = widget_channels_env)) {
                do.call(func, list(key, value, channel_id))
            } else {
                content <- list()
                content[[key]] <- value
                temp_channel_data <- get_from_widget_channel_env_cache(cached_data_var)
                #Update value in cache if key already exists else create/append new
                if(key %in% names(temp_channel_data[[channel_id]])) {
                    temp_channel_data[[channel_id]][[key]] <- value
                } else {
                    temp_channel_data[[channel_id]] <- append(temp_channel_data[[channel_id]], content)
                }
                put_from_widget_channel_env_cache(cached_data_var, temp_channel_data)
            }
        },
        set = function(key, value) {
            update_key_action(get_from_widget_channel_env_cache("the_channels")$set, "channel_set_data", key, value)
        },
        watch = function(key, handler) {
            update_key_action(get_from_widget_channel_env_cache("the_channels")$watch, "channel_watch_data", key, handler)
        },
        initialize = function(chan) {
            channel_id <<- chan
        }
    )
)

#' channel user level accessor
#'
#' @export
channel <- function(chan='default'){
    return (Channel$new(chan))
}

#' Widget_Channels
#'
#' Description = Encapsulates a list of widget_channels like channel a,b,c,etc...
Widget_Channels <- R6Class(
    'Widget_Channels',
    inherit = Widget,
    public = list(
        #channel to function
        watch_handlers = list(),
        serializer = NULL,
        handle_change = function(data) {
            #if data has channel information
            #if channel in message use it, else default to 'default';
            #if name in mesaage
            #then full name is 'channel:name'
            #then check if have a handler and call

            channel <- 'default'
            if('channel' %in% names(data)) {
                channel <- data[['channel']]
            }
            if('name' %in% names(data)) {
                full_channel_name <- paste(channel, ":", data[['name']], sep = "")
                if(full_channel_name %in% names(self$watch_handlers)) {
                    handler <- self$watch_handlers[[full_channel_name]]
                    tryCatch({
                        #invoke the handler
                        handler(data$old_val, data$new_val)
                        #send OK message back
                        self$send_ok()
                    }, error = function(e) {
                        print(c("Error executing watch handler on channel: ", data[['channel']]))
                        print(e)
                    })
                } else {
                    #print(c(full_channel_name, " not in watch_handlers"))
                }
            }
        },
        set = function(key, value, channel_id='default') {
            attr <- paste(channel_id, ":", key, sep = "")
            serialized <- self$serializer$serialize(value)
            self$send_update(attr, serialized)
        },
        watch = function(key, handler, channel_id='default') {
            qualified_name <- paste(channel_id, ":", key, sep = "")
            self$watch_handlers[[qualified_name]] <- handler
        },
        handle_custom = function(msg) {
            if(!is.null(msg$event) && msg$event == 'change') {
                self$handle_change(msg$data)
            }
        },
        handle_request_state = function(msg) {
            #restore channels set data
            channel_set_cache_data <- get_from_widget_channel_env_cache("channel_set_data")
            channel_names <- names(channel_set_cache_data)
            for(channel_name in channel_names) {
                for(key in names(channel_set_cache_data[[channel_name]])) {
                    value <- channel_set_cache_data[[channel_name]][[key]]
                    self$set(key, value, channel_name)
                }
            }
            #restore channels watch data
            channel_watch_cache_data <- get_from_widget_channel_env_cache("channel_watch_data")
            channel_names <- names(channel_watch_cache_data)
            for(channel_name in channel_names) {
                for(key in names(channel_watch_cache_data[[channel_name]])) {
                    handler <- channel_watch_cache_data[[channel_name]][[key]]
                    self$watch(key, handler, channel_name)
                }
            }
            ## Clear cache as we have inited and established state
            put_from_widget_channel_env_cache("channel_set_data", list())
            put_from_widget_channel_env_cache("channel_watch_data", list())
        },
        initialize = function(comm, serializer) {
            assign("the_channels", self, envir = widget_channels_env)
            #initialize super class Widget
            super$initialize(comm)
            self$serializer <- serializer
        }
    )
)