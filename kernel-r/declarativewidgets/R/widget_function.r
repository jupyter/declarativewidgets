#' Widget_Function
#'
#' Description
Widget_Function <- setRefClass(
    'Widget_Function',
    fields = list(
        widgets = 'list'
    ),
    methods = list(
        initialize = function() {
            print("initializing widget_function!!!")
        }
    )
)