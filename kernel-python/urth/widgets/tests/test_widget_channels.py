# (c) Copyright Jupyter Development Team
# (c) Copyright IBM Corp. 2015

""" Tests for the widget_channels.py module """

import unittest
from IPython.kernel.comm import Comm
from unittest.mock import Mock

from ..widget_channels import *


class TestWidgetChannels(unittest.TestCase):

    def setUp(self):
        comm = Mock(spec=Comm)
        self.widget = Channels(comm=comm)
        self.chan = 'c'
        self.name = 'x'

        self.msg = {
            'event': 'change',
            'data': {
                'channel': self.chan,
                'name': self.name,
                'old_val': 1,
                'new_val': 2
            }
        }

        self.lst = []
        self.handler = lambda x, y: self.lst.extend([x, y])

    #### watch()
    def test_watch(self):
        """should execute a registered handler with arguments from message"""
        self.widget.watch(self.name, self.handler, self.chan)
        self.widget._handle_change_msg(None, self.msg)
        self.assertEqual(self.lst, [1, 2])

    def test_watch_bad_channel(self):
        """should not execute a handler given an unregistered channel"""
        self.widget._handle_change_msg(None, self.msg)
        self.assertEqual(self.lst, [])

    def test_watch_bad_name(self):
        """should not execute a handler given an unregistered name"""
        self.widget.watch("bad name", self.handler, self.chan)
        self.widget._handle_change_msg(None, self.msg)
        self.assertEqual(self.lst, [])

    def test_watch_array(self):
        """should execute a handler given an array type"""
        self.widget.watch(self.name, self.handler, self.chan)
        self.msg['data']['old_val'] = [1, 2]
        self.msg['data']['new_val'] = [3, 4]
        self.widget._handle_change_msg(None, self.msg)
        self.assertEqual(self.lst, [[1, 2], [3, 4]])

    def test_watch_dict(self):
        """should execute a handler given an dict type"""
        self.widget.watch(self.name, self.handler, self.chan)
        self.msg['data']['old_val'] = {"a": 1}
        self.msg['data']['new_val'] = {"b": "c"}
        self.widget._handle_change_msg(None, self.msg)
        self.assertEqual(self.lst, [{"a": 1}, {"b": "c"}])

