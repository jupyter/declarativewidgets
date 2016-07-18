# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.

from IPython.core.display import display, HTML

def explore(df):
    """Renders the urth-viz-explorer widget to the user output"""
    display(HTML("<link rel='import' href='urth_components/declarativewidgets-explorer/urth-viz-vega-explorer.html' \
                    is='urth-core-import' package='jupyter-incubator/declarativewidgets_explorer'> \
                    <urth-viz-vega-explorer ref='{}'></urth-viz-vega-explorer>".format(df)))
