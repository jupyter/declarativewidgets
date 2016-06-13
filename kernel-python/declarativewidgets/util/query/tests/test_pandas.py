""" Tests for the pandas.py module

"""

import unittest

from ..pandas import *


class TestFunctions(unittest.TestCase):

    def test_to_dict_agg(self):
        """should convert to dictionary"""

        arg_array = [
            {"op": "sum", "col": "col1"},
            {"op": "sum", "col": "col2"},
            {"op": "count", "col": "col2"}
        ]

        expected = {
            "col1": ["sum"],
            "col2": ["sum", "count"]
        }

        actual = to_dict_agg(arg_array)

        self.assertEqual(expected, actual)

    def test_to_single_column_names(self):
        """should remove whitespaces from column names"""

        arg_array = [
            ["col1", ""],
            ["col2", "count"],
            ["col3", "sum"]
        ]

        expected = [
            "col1",
            "col2_count",
            "col3_sum"
        ]

        actual = to_single_column_names(arg_array)

        self.assertEqual(expected, list(actual))
