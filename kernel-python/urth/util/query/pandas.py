# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.


def apply_query(df, query=[]):
    if query:
        new_df = df.copy()
        for queryitem in query:
            if queryitem['type'] == 'filter':
                new_df = handle_filter(new_df, queryitem['expr'])

            elif queryitem['type'] == 'group':
                new_df = handle_group(new_df, queryitem['expr'])

        return new_df

    else:
        return df


def handle_filter(df, fltr_expr):
    return df.query(fltr_expr)


def handle_group(df, grp_expr):
    """
    Handles a group expression
    :param df: a Pandas DataFrame
    :param grp_expr: a dict with the group expression structure
    :return:
    """
    group_cols = grp_expr['by']
    group_aggs = grp_expr['agg']

    df = df.groupby(group_cols).agg(to_dict_agg(group_aggs))

    # move index into columns
    for _ in df.index.names:
        df.reset_index(level=0, inplace=True)

    df.columns = map(lambda col: col[:-1] if col.endswith('_') else col, ['_'.join(col).strip() for col in df.columns.values])

    return df


def to_dict_agg(agg_array):
    """
    Utility to convert that array structure with the aggregation information into the map/dict style structure that
    is passed to pandas' agg function.

    from:
    [
        {"op": "sum", "col": "col1"},
        {"op": "sum", "col": "col2"},
        {"op": "count", "col": "col2"}
    ]

    to
    {
        "col1": ["sum"],
        "col2": ["sum", "count"]
    }
    :param agg_array: Array with aggregate operations structure
    :return: a dict version of the aggregate operations compatible with Pandas.agg
    """
    dict_agg = dict()
    for agg in agg_array:
        if agg["col"] in dict_agg:
            dict_agg[agg["col"]].append(agg["op"])
        else:
            dict_agg[agg["col"]] = [agg["op"]]
    return dict_agg