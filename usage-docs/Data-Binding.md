Declarative Widgets enables the creation of reactive notebooks by using a
powerful data binding system. Data binding systems provide synchronization of
changes to data values across multiple references to the data. In Declarative
Widgets, data binding facilitates intra-cell and inter-cell data synchronization
to drive function invocations, visualization updates and more to ease the
creation of an interactive notebook application.

The data binding system in Declarative Widgets is built on top of the
[data binding](https://www.polymer-project.org/1.0/docs/devguide/data-binding)
framework provided by the [Polymer Project](https://www.polymer-project.org/1.0/). An
understanding of the Polymer data binding framework is essential in being
proficient in composing notebooks using Declarative Widgets.

#### `urth-core-bind` Element

The Declarative Widgets [`urth-core-bind`](#urth-core-bind) element is a template
extension that manages and synchronizes the data binding system. Any code that
requires data binding in the notebook, must be wrapped in an `urth-core-bind`
extended template.

#### Basic Data Binding

The following code demonstrates the basics of Declarative Widgets data binding.

    <template is='urth-core-bind'>
        <div>Hello <span>[[user]]</span></div>
        <input value="{{user::input}}"></input>
    </template>

1. An `urth-core-bind` template is used to wrap some content that uses data binding.
2. The value of a data item named `user` is displayed. Note that the `[[variable_name]]` syntax denotes that this particular data binding is a read-only or [one-way](https://www.polymer-project.org/1.0/docs/devguide/data-binding#property-binding)
data binding.
3. An input field is used to allow for the value of `user` to be specified. The `{{variable_name}}` syntax denotes a read-write or  [two-way](https://www.polymer-project.org/1.0/docs/devguide/data-binding#property-binding)
data binding. Note that the `::input` syntax is necessary in this example since
the `<input>` element is a [native element](https://www.polymer-project.org/1.0/docs/devguide/data-binding#two-way-native)

When run in a notebook, and a value is placed in the input box, the data value
is automatically synchronized to the display area:

![Intra-cell data binding](images/Data-Binding-intracell.gif)

#### Inter-cell Binding

Data binding can also be synchronized across multiple cells. The `urth-core-bind`
element internally keeps track of data values on data channels so that any
`urth-core-bind` element in the notebook can have access and be synchronized
with the data values in another `urth-core-bind` element. The previous example
can be split so that the display and input of the value of `user` are contained
in independent cells that are linked by wrapping them in `urth-core-bind` templates:

![Inter-cell data binding](images/Data-Binding-intercell.gif)

#### Multiple Data Channels

The `urth-core-bind` element automatically synchronizes data on a default data
channel across all other `urth-core-bind` elements. In a notebook it may be
desirable to separate some sections of code into independent data channels. For
this reason the `channel` attribute is available to specify which data channel
to use for a given `urth-core-bind` element. Data changes made on a specified
data channel are independent of other data channels in the notebook.

This example illustrates how data keys on a data channel are independent of
the same key on a different channel. In this example two cells each prompt
for the input of a data value `user` and display the value of `user` from
the other data channel.

![Multiple data channels](images/Data-Binding-multiple.gif)

#### Reading and Modifying Data Channel Values

Each data value in the selected channel is accessible for reading or modification
easily from the `urth-core-bind` element through JavaScript. The data channel
keys are exposed as properties on the element and can be directly accessed.
To retrieve or modify a data value, simply obtain a reference to the associated
`urth-core-bind` element and get or set the property on the element reference.

Assuming the following `urth-core-bind` element is defined:

    %%html
    <template is="urth-core-bind" id="mytemplate">
        <div>Hello <span>[[user]]</span></div>
    </template>

The value of `user` could be set via JavaScript as shown below (Note: This is
dependent on the kernel currently in use supporting the `%%javascript` magic):

    %%javascript
    var bindElem = document.getElementById('mytemplate');
    bindElem.user = "Luke";

Additionally, kernel and JavaScript API are available to retrieve and modify
the properties defined in the data channel. Reference the
[Data Channels](#data-channels) documentation for more information.

#### Driving Function Invocations

A powerful feature of Declarative Widgets is the ability to invoke functions
defined in the kernel declaratively through the [`urth-core-function`](#urth-core-function)
element. Data binding can be used to automate the execution of kernel functions to
retrieve new results when a specified data value changes. Consider the following
example code:

    <template is="urth-core-bind">
        <urth-core-function ref="multiplyBy10"
                            arg-x="[[x]]"
                            result="{{y}}" auto>
        </urth-core-function>
        x: <input type="text" value="{{x::input}}"></input>
        <span>{{y}}</span>
    </template>

1. An `urth-core-function` element is defined which references a kernel
function to invoke. Data binding is setup to pass in the value of `x` and track
the result as `y`. The function is set to automatically invoke when the `arg-x`
value changes by specifying the `auto` parameter.
2. An input element is used to specify the value of `x`.
3. The result `y` of the function invocation is displayed.

Including the code in a notebook shows how when the input value is changed, the
kernel function is automatically invoked and the result is displayed. While this
example specifies the inputs, function invocation and result in a single cell,
each piece could be performed in separate cells as described in [Inter-cell Binding](#data-binding+inter-cell-binding).

![Function Invocation](images/Data-Binding-function.gif)
