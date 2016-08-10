# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.

from IPython.core.display import display, HTML
import pandas
import pyspark

unique_explore_id = 0

def stringify_properties(properties):
    properties_str = ""
    for k in properties:
        if type(properties[k]) == bool:
            if properties[k]:
                properties_str += k + " "
        else:
            properties_str += '{}="{}" '.format(k, str(properties[k]))
    return properties_str

def stringify_bindings(bindings):
    bindings_str = ""
    for k in bindings:
        bindings_str += '{}="{{{{{}}}}}"'.format(k, bindings[k])
    return bindings_str

def explore(df, channel='default', properties={}, bindings={}):
    """
    Renders the urth-viz-explorer widget to the user output
    If pandas.DataFrame assign with unique name to user namespace else use what was passed in the string

    Parameters
    ----------
    df                  The dataframe itself or the string representation of the variable
    channel             The channel to bind to defaulted to default
    properties          The properties e.g. {'selection-as-object': False, 'foo': 5}
    bindings            The bindings e.g. {'selection': 'sel'}
    """

    global unique_explore_id
    unique_explore_id += 1
    explore_df = "unique_explore_df_name_" + str(unique_explore_id)
    if isinstance(df, pandas.DataFrame) or isinstance(df, pyspark.sql.DataFrame):
        get_ipython().user_ns[explore_df] = df
    else:
        explore_df = df

    display(HTML(
        """<link rel='import' href='urth_components/declarativewidgets-explorer/urth-viz-explorer.html'
               is='urth-core-import' package='jupyter-incubator/declarativewidgets_explorer'>
           <template is="urth-core-bind" channel="{channel}">
               <urth-viz-explorer ref='{ref}' {props} {binds}></urth-viz-explorer>
           </template>"""
            .format(ref=explore_df, channel=channel,
                    props=stringify_properties(properties), binds=stringify_bindings(bindings))
    ))
