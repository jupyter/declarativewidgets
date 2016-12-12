context("test_df_queries")

#common querier
querier <- Querier$new()
#common df
letters <- c('A', 'B', 'C', 'C', 'C', 'D')
numbers <- c(1, 2, 3, 4, 5, 6)
df <- data.frame(letters, numbers)
df$letters <- as.character(df$letters)

#common spark_df
if (require(SparkR, quietly=TRUE)) {
    library(SparkR)
    sc <- sparkR.init()
    sqlContext <- sparkRSQL.init(sc)
    spark_df <- createDataFrame(sqlContext, df)
} else {
    message("The package SparkR is not installed. Tests will be skipped.")
}

test_that("apply_query_single_filter", {
    #note the filter equality difference on those query strings
    single_filter_query_df <- "[{\"type\":\"filter\",\"expr\":\"letters == 'C'\"}]"
    new_df <- querier$apply_query(df, fromJSON(single_filter_query_df))
    expect_equal(nrow(new_df), 3)

    if (exists("spark_df")) {
        single_filter_query_spark_df <- "[{\"type\":\"filter\",\"expr\":\"letters = 'C'\"}]"
        new_spark_df <- querier$apply_query(spark_df, fromJSON(single_filter_query_spark_df))
        expect_equal(nrow(collect(new_spark_df)), 3)
    }
})

test_that("apply_query_multiple_filters", {
    multiple_filter_query_df <- "[{\"type\":\"filter\",\"expr\":\"letters == 'C'\"}, {\"type\":\"filter\",\"expr\":\"numbers == '4'\"}]"
    new_df <- querier$apply_query(df, fromJSON(multiple_filter_query_df))
    expect_equal(nrow(new_df), 1)
    
    if (exists("spark_df")) {
        multiple_filter_query_spark_df <- "[{\"type\":\"filter\",\"expr\":\"letters = 'C'\"}, {\"type\":\"filter\",\"expr\":\"numbers = '4'\"}]"
        new_spark_df <- querier$apply_query(spark_df, fromJSON(multiple_filter_query_spark_df))
        expect_equal(nrow(collect(new_spark_df)), 1)
    }
})

test_that("apply_query_sort_ascending_false", {
    sort_query <- "[{\"type\":\"sort\",\"expr\":{\"by\":\"numbers\",\"ascending\":false}}]"
    query <- fromJSON(sort_query)

    new_df <- querier$apply_query(df, query)
    expect_equal(as.numeric(new_df$numbers[1]), 6)
    
    if (exists("spark_df")) {
        new_spark_df <- querier$apply_query(spark_df, query)
        expect_equal(as.numeric(collect(new_spark_df)$numbers[1]), 6)
    }
})

test_that("apply_query_sort_ascending_true", {
    sort_query <- "[{\"type\":\"sort\",\"expr\":{\"by\":\"numbers\",\"ascending\":true}}]"
    query <- fromJSON(sort_query)

    new_df <- querier$apply_query(df, query)
    expect_equal(as.numeric(new_df$numbers[1]), 1)
    
    if (exists("spark_df")) {
        new_spark_df <- querier$apply_query(spark_df, query)
        expect_equal(as.numeric(collect(new_spark_df)$numbers[1]), 1)
    }
})

test_that("apply_query_sort_and_filter", {
    sort_and_filter_query_df <- "[{\"type\":\"filter\",\"expr\":\"letters == 'C'\"}, {\"type\":\"sort\",\"expr\":{\"by\":\"numbers\",\"ascending\":false}}]"
    new_df <- querier$apply_query(df, fromJSON(sort_and_filter_query_df))
    expect_equal(as.numeric(new_df$numbers[1]), 5)
    
    if (exists("spark_df")) {
        sort_and_filter_query_spark_df <- "[{\"type\":\"filter\",\"expr\":\"letters = 'C'\"}, {\"type\":\"sort\",\"expr\":{\"by\":\"numbers\",\"ascending\":false}}]"
        new_spark_df <- querier$apply_query(spark_df, fromJSON(sort_and_filter_query_spark_df))
        expect_equal(as.numeric(collect(new_spark_df)$numbers[1]), 5)
    }
})

test_that("apply_query_groupby", {
    groupby_query <- "[{\"type\":\"group\",\"expr\":{\"by\":[\"letters\"],\"agg\":[{\"op\":\"sum\",\"col\":\"numbers\"},{\"op\":\"mean\",\"col\":\"numbers\"}]}}]"
    query <- fromJSON(groupby_query)

    new_df <- querier$apply_query(df, query)
    expect_equal(nrow(new_df), 4)
    expect_equal(as.numeric(new_df$sum_numbers[3]), 12)
    expect_equal(as.numeric(new_df$mean_numbers[3]), 4)
    
    if (exists("spark_df")) {
        new_spark_df <- querier$apply_query(spark_df, query)
        expect_equal(nrow(collect(new_spark_df)), 4)
        expect_equal(as.numeric(collect(new_spark_df)$sum_numbers[3]), 12)
        expect_equal(as.numeric(collect(new_spark_df)$mean_numbers[3]), 4)
    }
})

test_that("apply_query_groupby_and_filter", {
    groupby_and_filter_query_df <- "[{\"type\":\"filter\",\"expr\":\"letters == 'C'\"}, {\"type\":\"group\",\"expr\":{\"by\":[\"letters\"],\"agg\":[{\"op\":\"sum\",\"col\":\"numbers\"},{\"op\":\"mean\",\"col\":\"numbers\"}]}}]"
    new_df <- querier$apply_query(df, fromJSON(groupby_and_filter_query_df))
    expect_equal(nrow(new_df), 1)
    expect_equal(as.numeric(new_df$sum_numbers[1]), 12)
    expect_equal(as.numeric(new_df$mean_numbers[1]), 4)
    
    if (exists("spark_df")) {
        groupby_and_filter_query_spark_df <- "[{\"type\":\"filter\",\"expr\":\"letters = 'C'\"}, {\"type\":\"group\",\"expr\":{\"by\":[\"letters\"],\"agg\":[{\"op\":\"sum\",\"col\":\"numbers\"},{\"op\":\"mean\",\"col\":\"numbers\"}]}}]"
        new_spark_df <- querier$apply_query(spark_df, fromJSON(groupby_and_filter_query_spark_df))
        expect_equal(nrow(collect(new_spark_df)), 1)
        expect_equal(as.numeric(collect(new_spark_df)$sum_numbers[1]), 12)
        expect_equal(as.numeric(collect(new_spark_df)$mean_numbers[1]), 4)
    }
})