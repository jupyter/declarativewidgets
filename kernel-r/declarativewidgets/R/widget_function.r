#' @include widget.r serializer.r
NULL
#' Widget_Function
#'
#' Description
Widget_Function <- R6Class(
    'Widget_Function',
    inherit = Widget,
    public = list(
        serializer = NULL,
        limit = NULL,
        function_name = NULL,
        handle_backbone = function(msg) {
            msg_limit <- msg$sync_data$limit
            msg_name <- msg$sync_data$function_name
            if(!is.null(msg_name)) {
                self$register_function(msg_name)
            } else {
                log_info("No name value provided for Widget_Function")
            }
            if(!is.null(msg_limit)) {
                self$register_limit(msg_limit)
            } else {
                log_info("No limit value provided for Widget_Function")
            }
        },
        handle_custom = function(msg) {
            if(!is.null(msg$event)) {
                if(msg$event == 'sync') {
                    self$send_signature(self$function_name, self$limit)
                } else if (msg$event == 'invoke') {
                    self$handle_invoke(msg, self$function_name, self$limit)
                } else {
                    log_info(paste("Unhandled custom event: ", msg$event))
                }
            } else {
                log_info("No event value in custom Widget_Dataframe comm message")
            }
        },
        handle_invoke = function(msg, func_name, limit) {
            if(!is.null(msg$args)) {
                result <- NULL
                tryCatch({
                    result <- self$invoke_function(func_name, msg$args, limit)
                    self$send_update("result", result)
                    self$send_ok()
                }, error = function(e) {
                    err_msg <- paste("Error invoking function", func_name)
                    self$send_error(err_msg)
                    log_error(e)
                    log_error(err_msg)
                })
            } else {
                err_msg <- "No arguments were provided for Widget_Function invocation!"
                self$send_error(err_msg)
                log_info(err_msg)
            }
        },
        register_function = function(func_name) {
            self$function_name <- func_name
            response <- self$send_signature(func_name, self$limit)
            self$handle_function_response(response)
        },
        invoke_function = function(func_name, args, limit=self$limit) {
            #get function from user/global env
            func <- get(func_name, envir = .GlobalEnv)
            #resolve args from defined env with args from client
            converted_args <- self$convert_args(func, args)
            #call the function with the list of args (convert_args is a list of values in order)
            result <- do.call(func, converted_args)
            serialized_result <- self$serializer$serialize(result)
            return (serialized_result)
        },
        #used to resolve the difference between the default variable values of the function
        #with the variable values from the client args sent over
        convert_args = function(func, args) {
            func_param_list <- formals(func)
            the_converted_args <- list()
            for (param in names(func_param_list)) {
                #is_required when variable does not have a default value
                is_required <- self$func_variable_is_required(func_param_list[[param]])
                client_arg <- NULL
                if(is_required) {
                    #variable is required/does not have a default value so
                    #we must use the arg passed from the client because nothing else
                    client_arg <- args[[param]]
                } else {
                    if(param %in% names(args)) {
                        #not required, but client passed in a value so use it rather than the default value
                        client_arg <- args[[param]]
                    } else {
                        #not required as has a default value and no client arg passed in, so use default value
                        client_arg <- func_param_list[[param]]
                    }
                }
                switch (
                    class(func_param_list[[param]]),
                        numeric     = the_converted_args[[param]] <- as.numeric(client_arg),
                        character   = the_converted_args[[param]] <- as.character(client_arg),
                        logical     = the_converted_args[[param]] <- as.logical(client_arg),
                        list        = the_converted_args[[param]] <- client_arg,
                        the_converted_args[[param]] <- client_arg
                )
            }
            return (the_converted_args)
        },
        send_signature = function(func_name, limit) {
            signature <- self$get_signature(func_name)
            if(!is.null(signature)) {
                self$send_update("signature", signature)
                return (TRUE)
            } else {
                err_msg <- paste("Could not determing signature for function:", func_name)
                log_info(err_msg)
                return (err_msg)
            }
        },
        #is required if variable from the function definition does not have a default value
        func_variable_is_required = function(var) {
            return (var == '' && class(var) == 'name')
        },
        get_signature = function(func_name) {
            names <- list()
            tryCatch({
                func <- get(func_name, envir = .GlobalEnv)
                func_param_list <- formals(func)
                for(param in names(func_param_list)) {
                    names[[param]] <- list()

                    #required if default value not provided/empty string and class is name
                    is_required <- self$func_variable_is_required(func_param_list[[param]])
                    names[[param]][['required']] <- is_required

                    if(!is_required) {
                        names[[param]][['value']] <- func_param_list[[param]]
                    } else {
                        names[[param]][['value']] <- list()
                    }
                    switch (
                        class(func_param_list[[param]]),
                        numeric     = names[[param]][['type']] <- "Number",
                        character   = names[[param]][['type']] <- "String",
                        logical     = names[[param]][['type']] <- "Boolean",
                        list        = names[[param]][['type']] <- "Array",
                        names[[param]][['type']] <- class(func_param_list[[param]])
                    )
                }
            }, error = function(e) {
                log_error(err_msg)
                log_error(paste("Error getting signature of function:", func_name))
            })
            return (names)
        },
        initialize = function(comm, serializer) {
            super$initialize(comm)
            self$serializer <- serializer
        }
    )
)