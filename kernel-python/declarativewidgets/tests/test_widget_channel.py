# (c) Copyright Jupyter Development Team
# (c) Copyright IBM Corp. 2015

""" Tests for the Channel class in widget_channels.py module """

import unittest

try:
    from unittest.mock import Mock
except ImportError as e:
    from mock import Mock

from ipykernel.comm import Comm
from .. import widget_channels
from ..widget_channels import Channels, Channel
from collections import defaultdict


class TestWidgetChannel(unittest.TestCase):

    # Executed before each test
    def setUp(self):
        # Reset the globals in widget_channels for each test.
        widget_channels.the_channels = None
        widget_channels.channel_data = defaultdict(dict)
        widget_channels.channel_watchers = defaultdict(dict)

        # Setup the channel object and inputs/handlers for each test
        channel = 'c'
        self.widget = Channel(chan=channel)
        self.name = 'x'
        self.msg = {
            'event': 'change',
            'data': {
                'channel': channel,
                'name': self.name,
                'old_val': 1,
                'new_val': 2
            }
        }

        self.lst = []
        self.handler = lambda x, y: self.lst.extend([x, y])

    #### watch()
    def test_watch(self):
        """should call watch handler when change is made"""
        comm = Mock(spec=Comm)
        channels = Channels(comm=comm)
        self.widget.watch(self.name, self.handler)
        channels._handle_change_msg(None, self.msg, None)
        self.assertEqual(self.lst, [1, 2])

    def test_watch_before_channels(self):
        """should store registered handler which is executed after Channels is instantiated and a change is made"""
        self.widget.watch(self.name, self.handler)
        comm = Mock(spec=Comm)
        channels = Channels(comm=comm)
        channels._handle_change_msg(None, self.msg, None)
        self.assertEqual(self.lst, [1, 2])

    def test_watch_bad_name(self):
        """should not execute a handler registered for a different property"""
        self.widget.watch("bad name", self.handler)
        comm = Mock(spec=Comm)
        channels = Channels(comm=comm)
        channels._handle_change_msg(None, self.msg, None)
        self.assertEqual(self.lst, [])

    def test_watch_array(self):
        """should execute a handler given an array type"""
        comm = Mock(spec=Comm)
        channels = Channels(comm=comm)
        self.widget.watch(self.name, self.handler)
        self.msg['data']['old_val'] = [1, 2]
        self.msg['data']['new_val'] = [3, 4]
        channels._handle_change_msg(None, self.msg, None)
        self.assertEqual(self.lst, [[1, 2], [3, 4]])

    def test_watch_array_before_channels(self):
        """should execute a handler given an array type when watch is registered before Channels instantiated"""
        self.widget.watch(self.name, self.handler)
        self.msg['data']['old_val'] = [1, 2]
        self.msg['data']['new_val'] = [3, 4]
        comm = Mock(spec=Comm)
        channels = Channels(comm=comm)
        channels._handle_change_msg(None, self.msg, None)
        self.assertEqual(self.lst, [[1, 2], [3, 4]])

    def test_watch_dict(self):
        """should execute a handler given an dict type"""
        comm = Mock(spec=Comm)
        channels = Channels(comm=comm)
        self.widget.watch(self.name, self.handler)
        self.msg['data']['old_val'] = {"a": 1}
        self.msg['data']['new_val'] = {"b": "c"}
        channels._handle_change_msg(None, self.msg, None)
        self.assertEqual(self.lst, [{"a": 1}, {"b": "c"}])

    def test_watch_dict_before_channels(self):
        """should execute a handler given an dict type when watch is registered before Channels instantiated"""
        self.widget.watch(self.name, self.handler)
        self.msg['data']['old_val'] = {"a": 1}
        self.msg['data']['new_val'] = {"b": "c"}
        comm = Mock(spec=Comm)
        channels = Channels(comm=comm)
        channels._handle_change_msg(None, self.msg, None)
        self.assertEqual(self.lst, [{"a": 1}, {"b": "c"}])

    ### set()
    def test_set(self):
        """should call set on the channels model"""
        MockSet = Mock()
        Channels.set = MockSet
        comm = Mock(spec=Comm)
        Channels(comm=comm)
        self.widget.set(self.name, 'myvalue')
        self.assertEqual(MockSet.call_count, 1)
        MockSet.assert_called_with(self.name, 'myvalue', 'c')

    def test_set_before_channels(self):
        """should call set on the channels model when set is called before Channels instantiated"""
        self.widget.set(self.name, 'myvalue')
        MockSet = Mock()
        Channels.set = MockSet
        comm = Mock(spec=Comm)
        Channels(comm=comm)
        self.assertEqual(MockSet.call_count, 1)
        MockSet.assert_called_with(self.name, 'myvalue', 'c')

    def test_set_args(self):
        """should call set on the channels model with args"""
        MockSet = Mock()
        Channels.set = MockSet
        comm = Mock(spec=Comm)
        Channels(comm=comm)
        self.widget.set(self.name, 'myvalue', a = 'vala', b = 'valb')
        self.assertEqual(MockSet.call_count, 1)
        MockSet.assert_called_with(self.name, 'myvalue', 'c', a = 'vala', b = 'valb')

    def test_set_args_before_channels(self):
        """should call set on the channels model with args when set is called before Channels instantiated"""
        self.widget.set(self.name, 'myvalue', a = 'vala', b = 'valb')
        MockSet = Mock()
        Channels.set = MockSet
        comm = Mock(spec=Comm)
        Channels(comm=comm)
        self.assertEqual(MockSet.call_count, 1)
        MockSet.assert_called_with(self.name, 'myvalue', 'c', a = 'vala', b = 'valb')
