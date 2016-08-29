# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.

from IPython.core.display import display, HTML
import pandas
try:
    import pyspark
except ImportError:
    # TODO: LOG WARNING
    pass

unique_explore_id = 0

def check_pyspark_package():
    try:
        import pyspark
    except ImportError:
        return False
    return True

def stringify_property(property_key, property_value):
    if type(property_value) == bool:
        if property_value:
            return property_key
    else:
        return '{}="{}"'.format(property_key, str(property_value))

def stringify_properties(properties):
    return ' '.join(filter(None, map(lambda x: stringify_property(x, properties[x]), properties)))

def stringify_bindings(bindings):
    return ' '.join(map(lambda x: '{}="{{{{{}}}}}"'.format(x, bindings[x]), bindings))

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
    if isinstance(df, pandas.DataFrame) or (check_pyspark_package() and isinstance(df, pyspark.sql.DataFrame)):
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
