#' @include queriers.r
NULL

Querier <- R6Class(
    'Querier',
    public = list(
        querier_list = list(),
        apply_query = function(df, query) {
            print("querier apply_query")
            print(query)
            if(!is.null(query) && class(df) %in% names(self$querier_list)) {
                return (self$querier_list[[class(df)]]$apply_query(df, query))
            } else {
                print("df not in querier_list")
                return (df)
            }
        },
        initialize = function() {
            self$querier_list[["data.frame"]] <- DataFrame_Querier$new()
            self$querier_list[["DataFrame"]] <- Spark_DataFrame_Querier$new()
        }
    )
)