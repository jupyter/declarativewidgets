#' @include serializer.r

DataFrame_Serializer <- R6Class(
    'DataFrame_Serializer',
    inherit = Serializer,
    public = list(
        klass = function() {
            library(base)
            data_frame_class_name <- "data.frame"
            #package_contents <- ls("package:base")
            #index_found <- match(data_frame_class_name, package_contents)
            #if(!is.na(index_found)) {
            #    data_frame_class_name <- package_contents[index_found]
            #}
            return (data_frame_class_name)
        },
        df_to_lists = function(df, limit) {
            rows <- list()
            for(i in 1:min(limit, nrow(df))) {
                row_elements <- list()
                for(j in 1:ncol(df)) {
                    row_elements <- append(row_elements, as.character(df[i,j]))
                }
                rows <- append(rows, list(row_elements))
            }
            return (rows)
        },
        serialize = function(obj, row_limit=100) {
            json <- list()
            json[['columns']] <- colnames(obj)
            json[['data']] <- self$df_to_lists(obj, row_limit)
            json[['index']] <- as.numeric(rownames(obj))
            return (json)
        },
        check_packages = function() {
            tryCatch({
                library(base)
            }, error = function(e) {
                return (False)
            })
            return (True)
        },
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
            library(SparkR)
            return ("DataFrame")
        },
        df_to_lists = function(df) {
            rows <- list()
            for(i in 1:nrow(df)) {
                row_elements <- list()
                for(j in 1:ncol(df)) {
                    row_elements <- append(row_elements, as.character(df[i,j]))
                }
                rows <- append(rows, list(row_elements))
            }
            return (rows)
        },
        serialize = function(obj, row_limit=100) {
            df <- collect(limit(obj, row_limit))
            json <- list()
            json[['columns']] <- colnames(df)
            json[['data']] <- self$df_to_lists(df)
            json[['index']] <- list(1:nrow(df))
            return (json)
        },
        check_packages = function() {
            tryCatch({
                library(SparkR)
            }, error = function(e) {
                return (False)
            })
            return (True)
        },
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
            library(base)
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
        check_packages = function() {
            tryCatch({
                library(base)
            }, error = function(e) {
                return (False)
            })
            return (True)
        },
        initialize = function() {
            #initialize must exists on this class
        }
    )
)