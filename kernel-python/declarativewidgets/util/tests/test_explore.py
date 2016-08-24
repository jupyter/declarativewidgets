# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.

"""
Tests for the explore.py module
"""

import unittest
import collections

from ..explore import *


class TestExplore(unittest.TestCase):

    def test_stringify_bindings(self):
        """should stringify bindings"""

        bindings = (('selection', 'sel'), ('foo', 'bar'))
        ordered_bindings = collections.OrderedDict(bindings)
        expected = "selection=\"{{sel}}\" foo=\"{{bar}}\""
        actual = stringify_bindings(ordered_bindings)
        self.assertEqual(expected, actual)

    def test_stringify_properties(self):
        """should stringify properties"""

        properties_false = (('selection-as-object', False), ('foo', 5))
        ordered_properties_false = collections.OrderedDict(properties_false)
        expected = "foo=\"5\""
        actual = stringify_properties(ordered_properties_false)
        self.assertEqual(expected, actual)

        properties_true = (('selection-as-object', True), ('foo', 5))
        ordered_properties_true = collections.OrderedDict(properties_true)
        expected = "selection-as-object foo=\"5\""
        actual = stringify_properties(ordered_properties_true)
        self.assertEqual(expected, actual)