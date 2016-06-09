# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.
import pyspark.sql.functions as F


def apply_query(df, query=[]):
    if query:
        new_df = df
        for queryitem in query:
            if queryitem['type'] == 'filter':
                new_df = handle_filter(new_df, queryitem['expr'])

            elif queryitem['type'] == 'group':
                new_df = handle_group(new_df, queryitem['expr'])

            elif queryitem['type'] == 'sort':
                new_df = handle_sort(new_df, queryitem['expr'])

        return new_df

    else:
        return df


def handle_filter( df, fltr_expr ):
    """
    Handles a filter expression
    :param df: a Pyspark DataFrame
    :param fltr_expr: a string filter expressions
    :return: filtered DataFrame
    """
    return df.filter(fltr_expr)


def handle_group( df, grp_expr ):
    """
    Handles a group expression
    :param df: a Pyspark DataFrame
    :param grp_expr: a dict with the group expression structure
    :return: grouped DataFrame
    """
    group_cols = grp_expr['by']
    group_aggs = grp_expr['agg']

    return df.groupby(group_cols).agg(*to_array_of_func_exprs(group_aggs))


def handle_sort(df, sort_expr):
    """
    Handles a sort expression
    :param df: a Pyspark DataFrame
    :param sort_expr: a dict with the sort expression structure
    :return: sorted DataFrame
    """
    sort_cols = sort_expr['by']
    sort_dir = sort_expr['ascending']

    return df.orderBy(sort_cols, ascending=sort_dir)


def to_array_of_func_exprs(agg_array):
    return map(F.expr, to_array_of_func_exprs_string(agg_array))


def to_array_of_func_exprs_string(agg_array):
    return ["{0}({1})".format(x["op"], x["col"]) for x in agg_array]
