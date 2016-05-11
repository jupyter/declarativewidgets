#' @include widget.r serializer.r querier.r
NULL
#' Widget_Dataframe
#'
#' Description
Widget_Dataframe <- R6Class(
    'Widget_Dataframe',
    inherit = Widget,
    public = list(
        serializer = NULL,
        limit = NULL,
        query = NULL,
        variable_name = NULL,
        querier = NULL,
        handle_backbone = function(msg) {
            msg_limit <- msg$sync_data$limit
            msg_name <- msg$sync_data$variable_name
            msg_query <- msg$sync_data$query
            if(!is.null(msg_limit)) {
                self$register_limit(msg_limit)
            } else {
                print("No limit value provided for Widget_Dataframe")
            }
            if(!is.null(msg_name)) {
                self$register_name(msg_name)
            } else {
                print("No name value provided for Widget_Dataframe")
            }
            if(!is.null(msg_query)) {
                self$register_query(msg_query)
                print(msg_query)
            } else {
                print("No name query provided for Widget_Dataframe")
            }
        },
        handle_custom = function(msg) {
            if(!is.null(msg$event)) {
                if(msg$event == 'sync') {
                    print("handle_custom")
                    self$serialize_and_send(self$variable_name, self$limit, self$query)
                } else {
                    print(c("Unhandled custome event: ", msg$event))
                }
            } else {
                print("No event value in custom Widget_Dataframe comm message")
            }
        },
        df_in_interpreter = function(name) {
            tryCatch({
                eval(name)
            }, error = function(e) {
                return (FALSE)
            })
            return (TRUE)
        },
        serialize_and_send = function(name, limit, query = list()) {
            if(self$df_in_interpreter(name)) {
                #apply query before sending over
                print("serialize_and_send")
                df_after_query <- self$querier$apply_query(get(name, envir = .GlobalEnv), query)
                serialized_df <- self$serializer$serialize(df_after_query, limit)
                self$send_update("value", serialized_df)
                return (TRUE)
            } else {
                print(c("DataFrame ", name, " not found! No sync message sent."))
                return (paste("Dataframe", name, "not found!"))
            }
        },
        register_name = function(name) {
            self$variable_name <- name
            response <- self$serialize_and_send(name, self$limit)
            self$handle_function_response(response)
        },
        register_limit = function(limit) {
            self$limit <- limit
        },
        register_query = function(query) {
            print("register_query")
            print(query)
            self$query <- fromJSON(query)
            response <- self$serialize_and_send(name, self$limit, self$query)
            self$handle_function_response(response)
        },
        initialize = function(comm, serializer, querier) {
            #initialize super class Widget and serializer
            super$initialize(comm)
            self$serializer <- serializer
            self$querier <- querier
        }
    )
)