# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.

""" A module containing serializers used by the Serializer.

To define a new serializer, create a subclass of BaseSerializer and
provide implementations of its methods. The subclass will be registered, i.e.
added to serializer_map, automatically.

The registered serializers can be used through the serializer_map variable.

Examples
--------
>>> f = Foo()
>>> serialized_f = serializer_map[Foo](f)

NOTE: Due to changed syntax for meta-classes, this is not compatible with
      Python 2.x
"""

import sys
import json
import re

from .serializer_registrar import SerializerRegistrar

if sys.version_info[0] == 2:
    from .base_serializer_py2 import BaseSerializer
else:
    from .base_serializer_py3 import BaseSerializer

def normalize_type(data_type):
    """Normalizes pandas/pyspark type names to the equivalent client side type names"""
    # disregard/normalize precision of the data_type
    data_type = re.sub("32|64(.*)", "", data_type)
    return {
        'int': 'Number',
        'bigint': 'Number',
        'float': 'Number',
        'double': 'Number',
        'bool': 'Boolean',
        'boolean': 'Boolean',
        'string': 'String',
        'datetime': 'Date',
        'date': 'Date'
    }.get(data_type, "Unknown")

class PandasSeriesSerializer(BaseSerializer):
    @staticmethod
    def klass():
        import pandas
        return pandas.Series

    @staticmethod
    def serialize(obj, **kwargs):
        # Default to index orientation
        # {index -> {column -> value}}
        date_format = kwargs.get('date_format', 'iso')
        return json.loads(obj.to_json(orient='index', date_format=date_format))

    @staticmethod
    def check_packages():
        try:
            import pandas
        except ImportError:
            return False
        return True

class PandasDataFrameSerializer(BaseSerializer):
    """A serializer for pandas.DataFrame"""

    @staticmethod
    def klass():
        import pandas
        return pandas.DataFrame

    @staticmethod
    def serialize(obj, **kwargs):
        limit = kwargs.get('limit', 100)
        # Default to split orientation
        # {index -> [index], columns -> [columns], data -> [values]}
        date_format = kwargs.get('date_format', 'iso')
        df_dict = json.loads(obj[:limit].to_json(orient='split', date_format=date_format))
        df_dict['columnTypes'] = kwargs.get('columnTypes', [str(x) for x in obj.dtypes.tolist()])
        df_dict['columnTypes'] = [normalize_type(x) for x in df_dict['columnTypes']]
        for i in range(0, len(df_dict['columnTypes'])):
            if df_dict['columnTypes'][i] == "Date":
                for j in range(0, len(df_dict['data'])):
                    #If this date element has no timezone drop the t/z from the serialized element
                    #Note on filter where rows are dropped we must associate the dict index with the original df index
                    row_index_in_obj = obj[df_dict['columns'][i]].index[j]
                    date_element = obj[df_dict['columns'][i]][row_index_in_obj]
                    if not hasattr(date_element, 'tzinfo') or date_element.tzinfo is None:
                        df_dict['data'][j][i] = re.sub("T|Z", " ", df_dict['data'][j][i])
        return df_dict

    @staticmethod
    def check_packages():
        try:
            import pandas
        except ImportError:
            return False
        return True


class MplFigureSerializer(BaseSerializer):
    """A serializer for matplotlib.pyplot.Figure"""

    @staticmethod
    def klass():
        import matplotlib.pyplot
        return matplotlib.pyplot.Figure

    @staticmethod
    def serialize(obj, **kwargs):
        import base64
        from io import BytesIO
        io = BytesIO()
        obj.savefig(io, format="png")
        prefix = "data:image/png;base64,"
        b64 = base64.encodebytes(io.getvalue())
        data = b64.decode("ascii").replace("\n", "")
        return prefix + data

    @staticmethod
    def check_packages():
        try:
            import matplotlib.pyplot
            import base64
            from io import BytesIO
        except ImportError:
            return False
        return True

class SparkDataFrameSerializer(BaseSerializer):
    """A serializer for Spark DataFrames."""

    @staticmethod
    def klass():
        import pyspark
        import pandas
        return pyspark.sql.DataFrame

    @staticmethod
    def serialize(obj, **kwargs):
        import pandas

        df = pandas.DataFrame.from_records(
            obj.limit(kwargs.get('limit', 100)).collect(), columns=obj.columns)

        #recover columnTypes from the original object before it is collected/converted
        columnTypes = [str(x[1]) for x in obj.dtypes]
        return PandasDataFrameSerializer.serialize(df, columnTypes=columnTypes, **kwargs)

    @staticmethod
    def check_packages():
        try:
            import pyspark
            import pandas
        except ImportError:
            return False
        return True
