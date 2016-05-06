# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.
import json


def pandas_apply_query(df, query=[]):
    if query:
        new_df = df.copy()
        for queryitem in query:
            if queryitem['type'] == 'filter':
                new_df = new_df.query(queryitem['expr'])

            elif queryitem['type'] == 'group':
                group_cols = queryitem['expr']['by']
                group_aggs = queryitem['expr']['agg']

                new_df = new_df.groupby(group_cols).agg(group_aggs)

                # move index into columns
                for _ in new_df.index.names:
                    new_df.reset_index(level=0, inplace=True)

                new_df.columns = map(lambda col: col[:-1] if col.endswith('_') else col, ['_'.join(col).strip() for col in new_df.columns.values])

        return new_df

    else:
        return df


def spark_apply_query(df, query):
    return df


def apply_query(df, query):
    return pandas_apply_query(df, query)
