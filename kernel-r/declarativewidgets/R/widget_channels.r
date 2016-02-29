#' @include widget.r
NULL

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
        set = function(key, value) {
            the_channels$set(key, value, channel_id)
        },
        watch = function(key, handler) {
            the_channels$watch(key, handler, channel_id)
        },
        initialize = function(chan) {
            channel_id <<- chan
        }
    )
)

the_channel <- function(chan='default'){
    return (Channel$new(chan))
}

#' Widget_Channels
#'
#' Description = Encapsulates a list of widget_channels like channel a,b,c,etc...
Widget_Channels <- R6Class(
    'Widget_Channels',
    inherit = Widget,
    #serializer = Serializer,
    public = list(
        #channel to function
        watch_handlers = list(),
        handle_change = function(data) {
            print("received handle_change_msg")
            print(data)
            #if data has channel information
            #if channel in message use it, else default to 'default';
            #if name in mesaage
            #then full name is 'channel:name'
            #then check if have a handler and call

            channel <- 'default'
            if('channel' %in% names(data)) {
                channel <- data[['channel']]
            }
            print(c("channel: ", channel))
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
                    print(c(full_channel_name, " not in watch_handlers"))
                }
            }
        },
        set = function(key, value, channel_id) {
            attr <- paste(channel_id, ":", key, sep = "")
            self$send_update(attr, value)
        },
        watch = function(key, handler, channel_id='default') {
            qualified_name <- paste(channel_id, ":", key, sep = "")
            print(c("qualified_name in watch assigned: ", qualified_name))
            self$watch_handlers[[qualified_name]] <- handler
        },
        handle_custom = function(msg) {
            print("handle_msg in Widget_Channels")
            if(!is.null(msg$event) && msg$event == 'change') {
                self$handle_change(msg$data)
            }
        },
        initialize = function(comm) {
            print("Initializing Widget_Channels")
            #expose channel to global env
            assign("the_channels", self, envir = .GlobalEnv)
            assign("channel", the_channel, envir = .GlobalEnv)
            #initialize super class Widget
            super$initialize(comm)
        }
    )
)