#' @include widget_channels.r widget_function.r widget_dataframe.r serializer.r querier.r
NULL

setClassUnion('CommOrNULL', members = c('Comm', 'NULL'))

#' Widget
#'
#' Base Widget class
#'
#' @export
Widget <- R6Class("Widget",
    public = list(
        comm = NA,
        initialize = function(comm) {
            if (!missing(comm)) self$comm <- comm

            self$comm$on_msg(self$handle_msg)
            self$comm$on_close(self$handle_close)
        },
        handle_custom_content = function(msg) {
            if(!is.null(msg$content)) {
                #handle_custom for widget_X
                self$handle_custom(msg$content)
            } else {
                print(c('No content in custom message', msg))
            }
        },
        handle_backbone_content = function(msg) {
            if(!is.null(msg)) {
                #handle_backbone for widget_X
                self$handle_backbone(msg)
            } else {
                print(c('No content in backbone message', msg))
            }
        },
        handle_request_state = function(msg) {
            msg <- list()
            msg[["method"]] <- "update"
            state_list <- list()
            msg[["state"]] <- state_list
            self$send(msg)
        },
        handle_request_state_content = function(msg) {
            if(!is.null(msg)) {
                #handle_request_state for widget_X
                self$handle_request_state(msg)
            } else {
                print(c('No content in request_state message', msg))
            }
        },
        handle_msg = function(msg) {
            switch(
                msg$method,
                backbone    = self$handle_backbone_content(msg),
                custom      = self$handle_custom_content(msg),
                request_state = self$handle_request_state_content(msg),
                print(c('Got unhandled msg type:', msg$method))
            )
        },
        handle_close = function(msg) {
            print("handle_close in Widget")
        },
        #Used by all widgets to send back a response indicating that
        #the result was successfully serialized and sent to the client
        handle_function_response = function(response) {
            if(class(response) == "character") {
                self$send_error(response)
            } else {
                self$send_ok()
            }
        },
        send_update = function(attribute, value) {
            msg <- list()
            msg[["method"]] <- "update"
            state_list <- list()
            state_list[[attribute]] <- value
            msg[["state"]] <- state_list
            self$send(msg)
        },
        send_status = function(status, msg="") {
            content <- list()
            content[["method"]] <- "update"
            state <- list()
            state[["__status__"]] <- list()
            state[["__status__"]][["status"]] <- status
            state[["__status__"]][["msg"]] <- msg
            state[["__status__"]][["timestamp"]] <- as.numeric(as.POSIXct(Sys.time()))*1000
            state_list <- list()
            state_list[["state"]] <- state
            send_message_content <- list(content, state_list)
            self$send(send_message_content)
        },
        send_error = function(msg) {
            self$send_status("error", msg)
        },
        send_ok = function(msg="") {
            self$send_status("ok", msg)
        },
        send = function(msg) {
            self$comm$send(msg)
        }
    )
)

create_widget_instance <- function(class_name, comm, serializer, querier) {
    switch(
        class_name,
        declarativewidgets.Function      = return (Widget_Function$new(comm = comm, serializer = serializer)),
        declarativewidgets.Channels      = return (Widget_Channels$new(comm = comm, serializer = serializer)),
        declarativewidgets.DataFrame    = return (Widget_Dataframe$new(comm = comm, serializer = serializer, querier = querier)),
        print(c('Got unhandled class_name:', class_name))
    )
}

#' Initialise and run the widget
#'
#' @export
initWidgets <- function() {
    serializer <- Serializer$new()
    querier <- Querier$new()
    target_handler <- function(comm, msg_data) {
        #get widget_class
        widget_class <- msg_data$widget_class
        #create the widget instance
        widget <- create_widget_instance(widget_class, comm, serializer, querier)
    }
    library(IRdisplay)
    library(IRkernel)
    comm_manager <- get("comm_manager", envir = as.environment("package:IRkernel"))()

    # Support for ipywidgets 4.x client
    comm_manager$register_target("ipython.widget", target_handler)

    # Support for ipywidgets 5.x client
    comm_manager$register_target("jupyter.widget", target_handler)

    # JavaScript code to load the declarative widgets extension.
    # Code sent to the front end from here may be executed after
    # extension initialization (iterative cell execution) or
    # before (Run All, Reload). This code works together with the
    # extension initialization to ensure that the required API is
    # available in all scenarios.
    #
    # Urth._initialized is a deferred that is resolved by the extension
    # initialization after the global Urth instance has been setup.
    # If extension initialization has not completed a new deferred is
    # initialized which extension initialization will resolve.
    #
    # Urth.whenReady is a public API defined by extension initialization
    # to delay javascript execution until dependencies have loaded. If
    # extension initialization has not completed a wrapper implementation
    # is setup which will invoke the real implementation when it is available.
    display_javascript('
        window.Urth = window.Urth || {};
        Urth._initialized = Urth._initialized || $.Deferred();
        Urth.whenReady = Urth.whenReady || function(cb) {
            Urth._initialized.then(function() {
                Urth.whenReady(cb);
            });
        };
        Urth.whenReady(function() { console.log("Declarative widgets connected.") });
    ')
}
