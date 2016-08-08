# (c) Copyright Jupyter Development Team

import unittest

try:
    from unittest.mock import Mock
except ImportError as e:
    from mock import Mock

from ipykernel.comm import Comm
from declarativewidgets.widget_function import Function

# Execute tests within an IPython instance
from IPython.testing.globalipapp import get_ipython
ip = get_ipython()


class TestWidgetFunction(unittest.TestCase):
    def setUp(self):
        comm = Mock(spec=Comm)
        self.fun = Function(comm=comm)

    def test_the_function(self):
        def mock_function(x):
            return x + 2

        ip.user_ns['mock_function'] = mock_function

        self.fun.function_name = 'mock_function'

        assert self.fun._the_function()(3) == 5

    def test_the_function_object_scope(self):
        class mock_class():
            def mock_class_function(self, x):
                return x + 3

        mock_object = mock_class()

        ip.user_ns['mock_object'] = mock_object

        self.fun.function_name = 'mock_object.mock_class_function'

        assert self.fun._the_function()(3) == 6
