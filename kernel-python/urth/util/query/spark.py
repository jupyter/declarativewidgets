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

        return new_df

    else:
        return df


def handle_filter( df, fltr_expr ):
    return df.filter(fltr_expr)


def handle_group( df, grp_expr ):
    group_cols = grp_expr['by']
    group_aggs = grp_expr['agg']

    return df.groupby(group_cols).agg(*to_array_of_func_exprs(group_aggs))


def to_array_of_func_exprs(agg_array):
    return map(F.expr, ["{0}({1})".format(x["op"],x["col"]) for x in agg_array])
