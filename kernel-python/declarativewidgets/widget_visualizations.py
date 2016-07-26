# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.

from IPython.core.display import display, HTML

def explore(df, channel='default', selection_var=None):
    """Renders the urth-viz-explorer widget to the user output"""
    display(HTML(
        """<link rel='import' href='urth_components/declarativewidgets-explorer/urth-viz-explorer.html'
               is='urth-core-import' package='jupyter-incubator/declarativewidgets_explorer'>
           <template is="urth-core-bind" channel="{channel}">
               <urth-viz-explorer ref='{ref}' {selection}></urth-viz-explorer>
           </template>""".format(ref=df, channel=channel, selection = 'selection="{{{{{}}}}}"'.format(selection_var) if selection_var else '')
    ))
