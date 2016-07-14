# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.

from IPython.core.display import display, HTML

def explore(df):
    """Renders the urth-viz-explorer widget to the user output"""
    display(HTML("<link rel='import' href='urth_components/urth-viz-vega/urth-viz-vega-explorer.html' \
                    is='urth-core-import' package='ibm-et/urth-viz-vega'> \
                    <urth-viz-vega-explorer multi-select ref='{}'></urth-viz-vega-explorer>".format(df)))