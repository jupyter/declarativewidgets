#' @include querier.r

#***TODO consider starting unit tests with testthat, as this is getting out of hand...

DataFrame_Querier <- R6Class(
    'DataFrame_Querier',
        inherit = Querier,
        public = list(
        apply_query = function(df, query) {
            print("applying df query")
            #***TODO fix hack on equal zero
            if(length(query) == 0) {
                return (df)
            } else {
                new_df <- df
                query_type_expr_map <- list()
                for(i in 1:length(query$type)) {
                    print(c("query_type is", query$type[[i]]))
                    expr <- if (length(query$type) > 1) query$expr[[i]] else query$expr
                    if(query$type[[i]] == "group") {
                        print(c("group class is ", class(expr)))
                        #***TODO - Refactor to convert the message then have one handle_group
                        if(class(expr) == "data.frame") {
                            new_df <- self$handle_group_data_frame(new_df, expr)
                        } else {
                            new_df <- self$handle_group_list(new_df, expr)
                        }
                    } else if(query$type[[i]] == "filter") {
                        new_df <- self$handle_filter(new_df, expr)
                    } else if(query$type[[i]] == "sort") {
                        new_df <- self$handle_sort(new_df, expr)
                    }
                }
                return (new_df)
            }
        },
        handle_sort = function(df, sort_expr) {
            print("sort query is")
            print(sort_expr)
            return (df[order(df[,c(as.character(sort_expr$by))], decreasing=(sort_expr$ascending == "FALSE")),])
        },
        handle_filter = function(df, filter_expr) {
            print("filter query is")
            print(filter_expr)
            #extract the filter column/value from the expression
            filter_exp_split <- unlist(strsplit(filter_expr, "[ ]"))
            column <- filter_exp_split[1]
            value <- gsub("'", "", filter_exp_split[3])
            print(column)
            print(value)
            return (df[df[,c(as.character(column))] == as.character(value), ])
        },
        handle_group_list = function(df, grp_expr) {
            cols <- list()
            for (i in 1:length(grp_expr$agg$op)) {
                if(i==1) {
                    #this is the first column name your grouping on only do once
                    temp_index <- (aggregate(df[,c(grp_expr$agg[2][,1][i])] ~ df[,c(as.character(grp_expr$by))], df, grp_expr$agg[1][,1][i])[1])
                    names(temp_index) <- as.character(grp_expr$by)
                    cols <- append(cols, temp_index)
                }
                temp_index <- (aggregate(df[,c(grp_expr$agg[2][,1][i])] ~ df[,c(as.character(grp_expr$by))], df, grp_expr$agg[1][,1][i])[2])
                names(temp_index) <- paste(grp_expr$agg[2][,1][i], "_", grp_expr$agg[1][,1][i], sep="")
                cols <- append(cols, temp_index)
            }
            new_df <- format(data.frame(cols))
            return (new_df)
        },
        handle_group_data_frame = function(df, grp_expr) {
            cols <- list()
            for(i in grp_expr$agg) {
                for (j in 1:length(i$op)) {
                    if(j==1) {
                        temp_index <- (aggregate(df[,c(i$col[j])] ~ df[,c(as.character(grp_expr$by))], df, i$op[j])[1])
                        names(temp_index) <- as.character(grp_expr$by)
                        cols <- append(cols, temp_index)
                    }
                    temp_col <- (aggregate(df[,c(i$col[j])] ~ df[,c(as.character(grp_expr$by))], df, i$op[j])[2])
                    names(temp_col) <- paste(i$col[j], "_", i$op[j], sep="")
                    cols <- append(cols, temp_col)
                }
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
        apply_query = function(df, query) {
            print("applying spark df query")
            print(query)
            return (df)
        },
        initialize = function() {
            #initialize must exists on this class
        }
    )
)