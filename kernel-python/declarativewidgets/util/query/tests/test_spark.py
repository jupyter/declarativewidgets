""" Tests for the pandas.py module

"""

import unittest

from ..spark import *


class TestFunctions(unittest.TestCase):

    def test_to_array_of_func_exprs_string(self):
        """should convert to array of function expressions"""

        arg_array = [
            {"op": "sum", "col": "col1"},
            {"op": "sum", "col": "col2"},
            {"op": "count", "col": "col2"}
        ]

        expected = [
            "sum(col1)",
            "sum(col2)",
            "count(col2)"
        ]

        actual = to_array_of_func_exprs_string(arg_array)

        self.assertEqual(expected, actual)

