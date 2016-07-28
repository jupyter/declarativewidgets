# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.

from IPython.core.display import display, HTML
import pandas

def explore(df, channel='default', selection_var=None):
    """
    Renders the urth-viz-explorer widget to the user output
    If pandas.DataFrame assign with unique name to user namespace else use what was passed in the string

    Parameters
    ----------
    df              The dataframe itself or the string representation of the variable
    channel         The channel to bind to defaulted to default
    selection_var   The selection variable by default not used/applied
    """

    explore_df = "unique_explore_df_name"
    if isinstance(df, pandas.DataFrame):
        get_ipython().user_ns[explore_df] = df
    else:
        explore_df = df

    selection = 'selection="{{{{{}}}}}"'.format(selection_var) if selection_var else ''

    display(HTML(
        """<link rel='import' href='urth_components/declarativewidgets-explorer/urth-viz-explorer.html'
               is='urth-core-import' package='jupyter-incubator/declarativewidgets_explorer'>
           <template is="urth-core-bind" channel="{channel}">
               <urth-viz-explorer ref='{ref}' {selection}></urth-viz-explorer>
           </template>""".format(ref=explore_df, channel=channel, selection=selection)
    ))
