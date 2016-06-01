#' @include serializers.r
NULL

Serializer <- R6Class(
    'Serializer',
    public = list(
        serializer_list = list(),
        serialize = function(obj, limit=100) {
            #if serializer for the class is registered use it else just return the object
            ref_klass <- class(obj)
            klass_name <- if (!is.na(ref_klass[2]) && ref_klass[2] == "R6") ref_klass[1] else ref_klass
            for(klass in names(self$serializer_list)) {
                if(klass_name == klass) {
                    return (self$serializer_list[[klass]](obj))
                }
            }
            return (obj)
        },
        register_serializer = function(a_serializer) {
            self$serializer_list[[a_serializer$klass()]] <- a_serializer$serialize
        },
        load_serializers = function() {
            self$register_serializer(DataFrame_Serializer$new())
            self$register_serializer(Spark_DataFrame_Serializer$new())
            self$register_serializer(Time_Series_Serializer$new())
        },
        initialize = function() {
            self$load_serializers()
            assign("register_serializer", self$register_serializer, envir = .GlobalEnv)
        }
    )
)