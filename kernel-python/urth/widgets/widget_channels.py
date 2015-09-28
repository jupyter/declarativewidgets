# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.

from IPython.html import widgets  # Widget definitions


# Global variable used to store the current Channels instance
the_channels = None


class Channels(widgets.Widget):
    """ A widget that provides an API for setting bound channel variables. """

    def __init__(self, value=None, **kwargs):
        self.log.info("Created a new Channels widget.")
        self.serializer = None
        global the_channels
        the_channels = self
        super(Channels, self).__init__(**kwargs)

    def set(self, key, value, chan='default', **kwargs):

        # Need to lazy import Serializers to avoid issue with matplotlib. The Kernel errors if the inline magic runs
        # after the modules get imported.
        if self.serializer is None:
            from urth.util.serializer import Serializer
            self.serializer = Serializer()

        attr = "{}:{}".format(chan, key)
        serialized = self.serializer.serialize(value, **kwargs)
        self._send_update(attr, serialized)

    def _send_update(self, attribute, value):
        msg = {
            "method": "update",
            "state": {
                attribute: value
            }
        }
        self._send(msg)


class Channel:
    """ Provides methods for interacting with a single channel."""

    def __init__(self, chan):
        self.chan = chan

    def set(self, key, value, **kwargs):
        global the_channels
        the_channels.set(key, value, self.chan, **kwargs)


def channel(chan='default'):
    """ API function for retrieving a single channel.

    Parameters
    ----------
    chan : string
        The channel name.

    Returns
    -------
    Channel
        The Channel object corresponding to the given channel name.

    """
    return Channel(chan)

