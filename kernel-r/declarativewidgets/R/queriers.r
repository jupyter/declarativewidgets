#' @include querier.r

DataFrame_Querier <- R6Class(
    'DataFrame_Querier',
    inherit = Querier,
    public = list(
        handle_sort = function(df, sort_expr) {
            return (df[order(df[,c(as.character(sort_expr$by))], decreasing=(as.character(sort_expr$ascending) == "FALSE")),])
        },
        handle_filter = function(df, filter_expr) {
            #extract the filter column/value from the expression
            filter_exp_split <- unlist(strsplit(filter_expr, "[ ]"))
            column <- filter_exp_split[1]
            value <- gsub("'", "", filter_exp_split[3])
            return (df[df[,c(as.character(column))] == as.character(value), ])
        },
        handle_group = function(df, grp_expr) {
            cols <- list()
            #this is the first column name your grouping on only do once
            temp_index <- (aggregate(df[,c(grp_expr$agg[2][,1][1])] ~ df[,c(as.character(grp_expr$by))], df, grp_expr$agg[1][,1][1])[1])
            names(temp_index) <- as.character(grp_expr$by)
            cols <- append(cols, temp_index)
            for (i in 1:length(grp_expr$agg$op)) {
                temp_index <- (aggregate(df[,c(grp_expr$agg[2][,1][i])] ~ df[,c(as.character(grp_expr$by))], df, grp_expr$agg[1][,1][i])[2])
                names(temp_index) <- paste(grp_expr$agg[2][,1][i], "_", grp_expr$agg[1][,1][i], sep="")
                cols <- append(cols, temp_index)
            }
            new_df <- format(data.frame(cols))
            return (new_df)
        },
        initialize = function() {
            #initialize must exists on this class
        }
    )
)

Spark_DataFrame_Querier <- R6Class(
    'Spark_DataFrame_Querier',
        inherit = Querier,
        public = list(
        handle_sort = function(df, sort_expr) {
            return (arrange(sparkFares, sort_expr$by, decreasing=(sort_expr$ascending == "FALSE")))
        },
        handle_filter = function(df, filter_expr) {
            return (filter(sparkFares, filter_expr))
        },
        handle_group = function(df, grp_expr) {
            agg_args <- list()
            col_names <- list()
            by_arg <- groupBy(df, as.character(grp_expr$by))
            agg_args <- append(agg_args, by_arg)
            for (i in 1:length(grp_expr$agg$op)) {
                temp_expr <- paste(grp_expr$agg[1][,1][i], "(", grp_expr$agg[2][,1][i], ")", sep="")
                agg_args <- append(agg_args, expr(temp_expr))
                temp_name <- paste(grp_expr$agg[2][,1][i], "_", grp_expr$agg[1][,1][i], sep="")
                col_names <- append(col_names, temp_name)
            }
            new_df <- collect(do.call(agg, agg_args))
            col_names <- append(names(new_df)[1], col_names)
            names(new_df) <- col_names
            return (new_df)
        },
        initialize = function() {
            #initialize must exists on this class
        }
    )
)