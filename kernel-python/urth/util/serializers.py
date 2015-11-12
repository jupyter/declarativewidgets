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

# serializer_map maps class names to serialization functions
serializer_map = {}


class SerializerRegistrar(type):
    """
    A metaclass used to register each class extending BaseSerializer.

    Specifically, adds a mapping of class to serialization function defined by
    the subclass of BaseSerializer.
    """
    def __init__(cls, name, bases, attrs):
        if cls.check_packages():
            serializer_map[cls.klass()] = cls.serialize


class BaseSerializer(object):
    """
    An abstract base class for serializers.

    Each class extending BaseSerializer should serialize a single class,
    whose name is returned by klass().

    Adding a new subclass of BaseSerializer in this package will automatically
    register the subclass for use by the Serializer.
    """

    __metaclass__ = SerializerRegistrar

    @staticmethod
    def klass():
        """The class that this serializer can serialize.

        Returns
        -------
        class
            class object representing the class that this serializer will
            serialize
        """
        pass

    @staticmethod
    def serialize(obj, **kwargs):
        """Serializes an object, assumed to have the class returned by `klass()`

        Parameters
        ----------
        obj : object
            The object to serialize. `obj`'s class will be equal to the class
            returned by `klass()`

        **kwargs : dict
            Allows for extra parameters to be sent to the serializer

        Returns
        -------
        obj
            The object in serialized form.
        """
        pass

    @staticmethod
    def check_packages():
        """Serialization may require using external packages.

        Override this function to check whether the packages needed
        to serialize this serializer's klass can be imported.

        Returns
        -------
        boolean
            True if necessary packages for serialization can be imported.
        """
        pass


class DataFrameSerializer(BaseSerializer):
    """A serializer for pandas.DataFrame"""

    @staticmethod
    def klass():
        import pandas
        return pandas.DataFrame

    @staticmethod
    def serialize(obj, **kwargs):
        json = {
            "columns": obj.columns.tolist(),
            "data": obj.head(kwargs.get('limit', 100)).values.tolist(),
            "index": obj.head(kwargs.get('limit', 100)).index.tolist()
        }
        return json

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

        json = {
            "columns": df.columns.tolist(),
            "data": df.values.tolist(),
            "index": df.index.tolist()
        }
        return json

    @staticmethod
    def check_packages():
        try:
            import pyspark
            import pandas
        except ImportError:
            return False
        return True
