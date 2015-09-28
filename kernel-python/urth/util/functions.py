# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.

""" Utilities related to functions.

"""

import inspect
import json


def apply_with_conversion(func, args):
    """ Apply the function with arguments converted to their inferred types.

    Types are inferred based on the types of default arguments and anntations.

    Parameters
    ==========
    func: function
        The function to apply.
    args: dict
        Mapping of argument name to argument value, in unconverted form
        (e.g. string).

    Returns
    =======
    value: function return type
        Output of the function invoked with typed arguments.

    Examples
    ========
    >> def foo(a: int):
    >>     return a

    >> args = {'a' : '3'}
    >> apply_with_conversion(foo, args)
    >> 3

    """
    spec = parameter_types(func)
    converted = convert_args(args, spec)
    return func(**converted)


def signature_spec(func):
    """ Detect parameter types and give the corresponding JavaScript type name.

    Types are inferred based on the types of default arguments and annotations.
    If a type does not map to a JavaScript type, returns the Python type name.
    Returns "NoneType" for parameters whose types cannot be inferred.

    Parameters
    ==========
    func: function
        The function to analyze.

    Returns
    =======
    names: dict
        Maps parameter name to dictionary of parameter info with a 'type' field.

    Examples
    ========
    >> def foo(a: int):
    >>     return a

    >> args = {'a' : '3'}
    >> parameter_type_js_names(foo, args)
    >> {'a' : {'type': 'Number'}}

    """
    types = parameter_types(func)
    names = {}
    for (param, tpe) in types.items():
        names[param] = {}
        if tpe == int or tpe == float:
            names[param]['type'] = "Number"
        elif tpe == str:
            names[param]['type'] = "String"
        elif tpe == bool:
            names[param]['type'] = "Boolean"
        elif tpe == list:
            names[param]['type'] = "Array"
        elif tpe == dict:
            names[param]['type'] = "Object"
        else:
            if tpe.__module__ == "builtins":
                names[param]['type'] = tpe.__name__
            else:
                names[param]['type'] = tpe.__module__ + "." + tpe.__name__

    # marked required parameters
    for param in required_parameter(func):
        names[param]['required'] = True

    default_values = _get_default_vals(func)

    for param, value in default_values.items():
        names[param]['value'] = value

    return names


def parameter_types(func):
    sig = inspect.signature(func)

    types = {}
    for (name, param) in sig.parameters.items():
        tpe = _param_type(param)
        types[name] = tpe

    return types


def required_parameter(func):
    sig = inspect.signature(func)

    required = []
    for (name, param) in sig.parameters.items():
        if not _has_default_val(param):
            required.append(name)

    return required


def convert_args(args, spec):
    converted = {}
    for (name, val) in args.items():
        if name in spec:
            converted[name] = _convert(val, spec[name], name)
    return converted


def _convert(val, tpe, name):
    try:
        if tpe == int:
            val = int(val)
        elif tpe == float:
            val = float(val)
        elif tpe == bool:
            val = bool(val)
        elif tpe == str:
            val = str(val)
        elif tpe == list:
            val = json.loads(val)
        elif tpe == dict:
            val = json.loads(val)
    except ValueError:
        raise ValueError("Value {} could not be converted to inferred type {} "
                         "for argument {}.".format(val, tpe, name))
    except TypeError:
        raise TypeError(
            "Value {} of type {} could not be converted to inferred "
            "type {} for argument {}.".format(val, type(val), tpe, name))
    return val


def _non_empty(val):
    return id(val) != id(inspect._empty)

def _param_type(param):
    # use the type of the default value if present
    if _has_default_val(param):
        return type(param.default)

    # use the annotation if it evaluates to a class
    if _non_empty(param.annotation) and inspect.isclass(param.annotation):
        return param.annotation

    # return NoneType if we cannot determine the type
    return type(None)

def _has_default_val(param):
    return _non_empty(param.default)

def _get_default_vals(func):
    sig = inspect.signature(func)

    default = {}
    for (name, param) in sig.parameters.items():
        if _has_default_val(param):
            default.update({name: param.default})

    return default
