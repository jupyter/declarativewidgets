#' @include serializer.r

serialize_element <- function(elem) {
    switch(
        class(elem),
        Date    = {
            options(digits.secs=3)
            return (format(elem, "%Y-%m-%dT%H:%M:%OSZ"))
        },
        integer = return (elem),
        numeric = return (elem),
        logical = return (elem),
        return (as.character(elem))
    )
}

get_df_column_types <- function(df) {
    class_info <- lapply(aDataFrame, class)
    type_info <- list()
    for (klass in class_info) {
        switch(
            klass,
            Date        = type_info <- append(type_info, "Date"),
            integer     = type_info <- append(type_info, "Number"),
            numeric     = type_info <- append(type_info, "Number"),
            logical     = type_info <- append(type_info, "Boolean"),
            character   = type_info <- append(type_info, "String"),
            type_info <- append(type_info, "Unknown")
        )
    }
    return (unlist(type_info))
}

requireTheNamespace <- function(requiredNamespace) {
    requireNamespaceResult <- requireNamespace(requiredNamespace, quietly = TRUE)
    if(!requireNamespaceResult) log_error(paste('Error requiring namespace: ', requireNamespaceResult))
}

DataFrame_Serializer <- R6Class(
    'DataFrame_Serializer',
    inherit = Serializer,
    public = list(
        klass = function() {
            requireTheNamespace("base")
            return ("data.frame")
        },
        df_to_lists = function(df, limit) {
            rows <- list()
            for(i in 1:min(limit, nrow(df))) {
                row_elements <- list()
                for(j in 1:ncol(df)) {
                    row_elements <- append(row_elements, serialize_element(df[i,j]))
                }
                rows <- append(rows, list(row_elements))
            }
            return (rows)
        },
        serialize = function(obj, row_limit=100) {
            json <- list()
            json[['columns']] <- colnames(obj)
            json[['columnTypes']] <- get_df_column_types(obj)
            json[['data']] <- self$df_to_lists(obj, row_limit)
            json[['index']] <- as.numeric(rownames(obj))
            return (json)
        },
        check_packages = function() requireNamespace('base', quietly = TRUE),
        initialize = function() {
            #initialize must exists on this class
        }
    )
)

Spark_DataFrame_Serializer <- R6Class(
    'Spark_DataFrame_Serializer',
    inherit = Serializer,
    public = list(
        klass = function() {
            requireTheNamespace("SparkR")
            return ("DataFrame")
        },
        df_to_lists = function(df) {
            rows <- list()
            for(i in 1:nrow(df)) {
                row_elements <- list()
                for(j in 1:ncol(df)) {
                    row_elements <- append(row_elements, serialize_element(df[i,j]))
                }
                rows <- append(rows, list(row_elements))
            }
            return (rows)
        },
        serialize = function(obj, row_limit=100) {
            df <- collect(limit(obj, row_limit))
            json <- list()
            json[['columns']] <- colnames(df)
            json[['columnTypes']] <- get_df_column_types(df)
            json[['data']] <- self$df_to_lists(df)
            json[['index']] <- list(1:nrow(df))
            return (json)
        },
        check_packages = function() requireNamespace('SparkR', quietly = TRUE),
        initialize = function() {
            #initialize must exists on this class
        }
    )
)

Time_Series_Serializer <- R6Class(
    'Time_Series_Serializer',
    inherit = Serializer,
    public = list(
        klass = function() {
            requireTheNamespace("base")
            return("ts")
        },
        index_to_list = function(a_ts) {
            index_list <- list()
            start <- tsp(a_ts)[1]
            for(i in 1:length(unclass(a_ts))) {
                index_list <- append(index_list, start)
                start <- start + (1/tsp(a_ts)[3])
            }
            return (index_list)
        },
        serialize = function(obj, limit=100) {
            json <- list()
            json[['data']] <- as.list(unclass(obj))
            json[['index']] <- self$index_to_list(obj)
            return (json)
        },
        check_packages = function() requireNamespace('base', quietly = TRUE),
        initialize = function() {
            #initialize must exists on this class
        }
    )
)
