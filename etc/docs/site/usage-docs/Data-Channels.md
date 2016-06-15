A key feature of Declarative Widgets is the [data binding](#data-binding)
framework that enables creation of dynamic, interactive notebooks. At the
core of the data binding framework is the concept of a data channel that
acts as a store of data keys and values in the notebook so that data can
be synchronized across all references. In Declarative Widgets the data channel
is implemented in the [`urth-core-channel`](#urth-core-channel) element.

## `urth-core-channel` element

As discussed in the [Data Binding](#data-binding+multiple-data-channels)
documentation, each [`urth-core-bind`](#urth-core-bind) element is back by a
data channel to synchronize changes. Internally the `urth-core-bind` element
uses an `urth-core-channel` element to manage data keys and values. The
`urth-core-channel` element can also be directly used in a notebook to obtain
a reference to channel data and initialize data, access the API and debug
the current state of the channel.

## Initialize Channel Data

Data channels are empty by default. In times when initial values are required,
use the [`urth-core-channel-item`](#urth-core-channel-item) element to initialize
data values.

    <urth-core-channel id='itemChannel' name='initialized'>
        <urth-core-channel-item
                key='myvar'
                value='awesome'>
        </urth-core-channel-item>
        <urth-core-channel-item
                key='isAwesome'
                value=true>
        </urth-core-channel-item>
        <urth-core-channel-item
                key='stuff'
                value='{ "json": true, "foo": "bar"}'>
        </urth-core-channel-item>
    </urth-core-channel>

The above example adds three keys and values for `myvar`, `isAwesome` and
`otherStuff` to the data channel named `initialized`. Notice how the `urth-core-channel-item` elements are contained within the `urth-core-channel`
element that they are initializing. The example below illustrates
how the channel data is initialized when the cell is executed:

![Initialize Data Channel](images/Data-Channels-initialize.gif)

## Channel API

Data channels have a set of API defined in each supported kernel and in
JavaScript which allows channel data to be set, retrieved and watched
programmatically.

### JavaScript Channel API

The JavaScript Channel API is defined directly on the [`urth-core-channel`](#urth-core-channel)
element. In order to use the JavaScript API there must be a channel element on
the page for the desired channel. The examples that follow assume the following
element is defined on the page:

    <urth-core-channel id="aChannel" name="a"></urth-core-channel>

#### Channel Set/Get

The JavaScript API to set and get the value of a data item on a channel are
defined as [`set(key, value)`](#urth-core-channel+method-set) and
[`get(key)`](#urth-core-channel+method-get) methods on the `urth-core-channel` element.
Invocations of the API should be passed as a callback function to the
[Urth.whenReady](#utility-functions+urth-whenready) to ensure that the
`urth-core-channel` API has been defined on the element:

    Urth.whenReady(function() {
        var aChannel = document.getElementById('aChannel');
        aChannel.set('color', 'blue');
    });

#### Channel Watch

The JavaScript API to watch and unwatch for changes to a data item on a channel are
defined as [`watch(key, handler)`](#urth-core-channel+method-watch) and
[`unwatch(key, handler)`](#urth-core-channel+method-unwatch) methods on the `urth-core-channel` element.
Invocations of the API should be passed as a callback function to the
[Urth.whenReady](#utility-functions+urth-whenready) to ensure that the
`urth-core-channel` API has been defined on the element:

    Urth.whenReady(function() {
        var aChannel = document.getElementById('aChannel');

        var handler = function(name, oldVal, newVal) {
            aChannel.set('old_color', oldVal);
        };
        aChannel.watch('color', handler);
    });

#### Example:

![JavaScript Channel API](images/Data-Channels-JS.gif)

### Python Channel API

The Python channel API is defined in the `channel` module of the `declarativewidgets`
package and must be imported into the notebook for the API to be available.
To enable the Python channel API, include and run a line like the following in the
notebook before attempting to use the API:

    from declarativewidgets import channel

The following examples assume the above line has been used to import the channel
API.

#### Channel Set

To set a value on a given channel, invoke the `channel` module with the name
of the channel and call the `set(key, value)` method:

    channel('a').set('color', 'blue')

#### Channel Watch

Channel data values can be watched for changes by invoking the `channel`
module with the name of the channel and calling the `watch(key, handler)` method
with the desired key and a callback function:

    channel('a').watch('color', handler)

#### Example:

![Python Channel API](images/Data-Channels-Python.gif)

### R Channel API

The Scala channel API is defined in the `channel` function of the `declarativewidgets`
library. Initializing the `declarativewidgets` library will define a global user level
function for `channel`.

#### Channel Set/Get

To set a value on a given channel, invoke the global `channel` function with the name
of the channel and then call the `$set(key, value)` function of the `channel` class:

    channel("a")$set("color", "blue")

#### Channel Watch

Channel data values can be watched for changes by invoking the global `channel`
function with the name of the channel and calling the `$watch(key, handler)` function
of the `channel` class with the desired key and a callback function:

    channel("a")$watch("color", handler)

#### Example:

![R Channel API](images/Data-Channels-R.gif)

### Scala Channel API

The Scala channel API is defined in the `channel` module of the `declarativewidgets`
package and must be imported into the notebook for the API to be available.
To enable the Scala channel API, include and run a line like the following in the
notebook before attempting to use the API:

    import declarativewidgets.channel

The following examples assume the above line has been used to import the channel
API.

#### Channel Set

To set a value on a given channel, invoke the `channel` module with the name
of the channel and call the `set(key, value)` method:

    channel('a').set('color', 'blue')

#### Channel Watch

Channel data values can be watched for changes by invoking the `channel`
module with the name of the channel and calling the `watch(key, handler)` method
with the desired key and a callback function:

    channel("a").watch("color", handler)

#### Example:

![Scala Channel API](images/Data-Channels-scala.gif)

## Saving, Reloading and Clearing

The data in a channel can be saved in and restored from the local browser
storage. Use the [`save`](#urth-core-channel+method-save) method to persist the current
channel state and the [`reload`](#urth-core-channel+method-reload) method to load
a previously saved state. The current data in the channel can be cleared with
the [`clear`](#urth-core-channel+method-clear) method.

> Note that all channels on the page are automatically saved when the notebook
> is saved and saved channel data automatically restored on reload of the page.

![Data Channel Save, Reload, Clear](images/Data-Channels-save.gif)

## Debug

Determining the current values of data on a given channel is useful in
debugging a notebook. For this reason the `urth-core-channel` element supports
a [`debug`](#urth-core-channel+property-debug) parameter which will cause the
element to generate a dynamically updated table of the current state of the data
in the channel. The table also includes a button to quickly and easily clear
all data in the channel.

![Data Channel Debug](images/Data-Channels-debug.gif)
