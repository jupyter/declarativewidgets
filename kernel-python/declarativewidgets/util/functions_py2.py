# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.

""" Utilities related to functions. This version is specific to Python 2. """

import inspect

def get_arg_spec(func):
    sig = inspect.getargspec(func)
    if "self" in sig.args:
        sig.args.remove("self")
    return sig

def parameter_types(func):
    sig = get_arg_spec(func)
    required = required_parameter(func)
    default = default_parameters(func)

    types = {}
    for arg in sig.args:
        if arg in required:
            types[arg] = type(None)
        else:
            types[arg] = type(default[arg])

    return types


def default_parameters(func):
    sig = get_arg_spec(func)

    if sig.defaults:
        return dict(zip(sig.args[-len(sig.defaults):], sig.defaults))
    else:
        return {}


def required_parameter(func):
    sig = get_arg_spec(func)

    if sig.defaults:
        return list(set(sig.args) - set(sig.args[-len(sig.defaults):]))
    else:
        return sig.args
