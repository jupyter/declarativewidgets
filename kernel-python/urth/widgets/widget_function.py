# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.

import inspect

from IPython.html import widgets # Widget definitions
from IPython.utils.traitlets import Integer, Unicode # Used to declare attributes of our widget
from IPython.core.getipython import get_ipython

from urth.util.serializer import Serializer
from urth.util.functions import apply_with_conversion, signature_spec


class Function(widgets.Widget):
    """
    A Widget for invoking a function on the kernel.
    """
    function_name = Unicode('', sync=True)
    limit = Integer(100, sync=True)

    def __init__(self, value=None, **kwargs):
        self.log.info("Created a new Function widget.")

        self.on_msg(self._handle_invoke_msg)
        self.shell = get_ipython()
        self.serializer = Serializer()
        super(Function, self).__init__(**kwargs)

    def _function_name_changed(self, old, new):
        self.log.info("Binding to function name {}...".format(new))
        signature = signature_spec(self._the_function())
        self._send_update("signature", signature)

    def _handle_invoke_msg(self, _, content):
        if content.get('event', '') == 'invoke':
            self._invoke(content.get('args', {}))

    def _the_function(self):
        return self.shell.user_ns[self.function_name]

    def _invoke(self, args):
        self.log.info("Invoking function {} with args {}...".format(
            self.function_name, args))
        result = apply_with_conversion(self._the_function(), args)
        serialized_result = self.serializer.serialize(result, limit=self.limit)
        self._send_update("result", serialized_result)

    def _send_update(self, attribute, value):
        msg = {
            "method": "update",
            "state": {
                attribute: value
            }
        }
        self._send(msg)
