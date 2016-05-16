#' @include queriers.r
NULL

Querier <- R6Class(
    'Querier',
    public = list(
        querier_list = list(),
        apply_query = function(df, query) {
            if(!is.null(query) && class(df) %in% names(self$querier_list)) {
                return (self$querier_list[[class(df)]]$apply_query_impl(df, query))
            } else {
                print("df not in querier_list")
                return (df)
            }
        },
        apply_query_impl = function(df, query) {
            if(length(query) == 0) {
                return (df)
            } else {
                new_df <- df
                query_type_expr_map <- list()
                for(i in 1:length(query$type)) {
                    expr <- if (length(query$type) > 1) query$expr[[i]] else query$expr
                    if(query$type[[i]] == "group") {
                        if(class(expr) == "data.frame") {
                            expr <- as.list(as.data.frame(t(expr)))[[1]]
                        }
                        new_df <- self$handle_group(new_df, expr)
                    } else if(query$type[[i]] == "filter") {
                        new_df <- self$handle_filter(new_df, expr)
                    } else if(query$type[[i]] == "sort") {
                        new_df <- self$handle_sort(new_df, expr)
                    }
                }
                return (new_df)
            }
        },
        initialize = function() {
            self$querier_list[["data.frame"]] <- DataFrame_Querier$new()
            self$querier_list[["DataFrame"]] <- Spark_DataFrame_Querier$new()
        }
    )
)