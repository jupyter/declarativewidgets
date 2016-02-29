#' Widget_Dataframe
#'
#' Description
Widget_Dataframe <- setRefClass(
    'Widget_Dataframe',
    fields = list(
        widgets = 'list'
    ),
    methods = list(
        initialize = function() {
            print("initializing widget_dataframe!!!")
        }
    )
)