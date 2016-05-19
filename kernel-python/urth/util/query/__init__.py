# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.

# Setting up query support

query_support_map = {}

try:
    from pandas import DataFrame
    from .pandas import apply_query as pandas_apply_query
    query_support_map[DataFrame] = pandas_apply_query
except ImportError:
    # TODO: LOG WARNING
    pass

try:
    import pyspark
    from .spark import apply_query as spark_apply_query
    query_support_map[pyspark.sql.DataFrame] = spark_apply_query
except ImportError:
    # TODO: LOG WARNING
    pass


def apply_query(df, query):
    return query_support_map[type(df)](df, query) if type(df) in query_support_map else df