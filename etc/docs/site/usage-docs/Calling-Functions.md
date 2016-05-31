Calling a function defined in the kernel by interacting with visual elements in the Notebook is one of the core features of DeclarativeWidgets. These are arbitrary functions that are defined in the top level scope of the kernel, using any of the supported languages. For example, these functions can be used to: 

* process an HTML form
* load data and make it available to the HTML
* transform data between a two visual elements (i.e a table selection and a map)
* start a Spark Streaming job
* and many more...

The `urth-core-function` element is what enables tying a function to the rest of the HTML template. It makes the function defined in the kernel seem like it is local to the HTML and allows other elements to set parameters and visualize the result of calling the function.

The following shows a basic example of what is necessary to use `urth-core-function`:

> TODO: Image of a simple use of a urth-core-function

```Python
def someFunction(a,b,c):
  return "something"
```

```html
%%HTML
<urth-core-function arg-a="..." arg-b="..." arg-c="..." result="..."></urth-core-function>
```

#### Connecting the element to the function

The `urth-core-function` element needs to know what function defined in the kernel it is representing. This is done by setting the `ref` property to the name of the function. That gives the element enough information to find the function in the kernel and know what parameters it needs. In the example above, the element is representing the function `someFunction`.

#### Setting parameters

All parameter to the function are exposed as element properties with the name `arg-<param name>`'. These can be set to literal values or bound to other elements using the data binding syntax `{{param}}`. In the above example, the parameter `a` can be set using the property `arg-a`. If any of the parameters have default values (if the language permits), then the corresponding property will have that value set by default.

#### Invoking the function

There are several ways to invoke the function using the `urth-core-function` element.

1. Directly calling the `invoke` method of the element using Javascript.
2. Placing an HTML or element as a child that fires a `click` event (i.e. a `button` or `a` element)
3. Setting the `auto` property and let the element automatically invoke the function when it detects the parameters are all set correctly

#### Using the results of the function

If the function defined in the kernel returns some value as part of its invocation, that value is available in the `result` property of the `urth-core-function` element. Typically, this property is used in a data binding and the value is visualized by some other element(s).

The content of the `result` property depends on what value type is return by the function the element represents. Basic types are supported, but other more complex types can also be returned. For example, functions can return DataFrames (see [here](Connecting-to-data#format-of-the-data) for DataFrame serialization).

For more detail information about the `urth-core-function` element, see the [api docs](http://jupyter-incubator.github.io/declarativewidgets/docs.html).