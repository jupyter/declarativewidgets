# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.

from IPython.html import widgets  # Widget definitions


class UrthWidget(widgets.Widget):
    """ A base class for Urth widgets. """

    def __init__(self, **kwargs):
        super(UrthWidget, self).__init__(**kwargs)

    def send_state(self, key=None):
        """Overrides the Widget send_state to prevent
        an unnecessary initial state message.
        """
        pass