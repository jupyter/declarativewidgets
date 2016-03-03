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
        serialize = function(obj, limit=100) {
            json <- list()
            json[['columns']] <- colnames(obj)
            json[['data']] <- self$df_to_lists(obj, limit)
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