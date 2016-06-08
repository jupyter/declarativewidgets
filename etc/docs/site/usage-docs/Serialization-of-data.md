Many of the features of DeclarativeWidgets involve moving data between the kernel and the browser. This transfer of data involved serializing objects in the kernel into JSON structures.

Some examples are:
* Return value from a function invocation
* Default values for function parameters
* Sending data through channels
* Getting the data from a DataFrame

#### Basic types

For the most part, may of the basic primitive types in the language of the kernel are supported, as well as basic collections types like Arrays, Sequences, and Maps. You can expect the following:

>TODO: show as table

* numerics -> Number
* booleans -> Booleans
* string -> Strings
* arrays and sequences -> []
* maps -> {}

#### Complex types

##### DataFrames
DataFrames from R, Pandas and Spark are serialized into the following structure:

```javascript
{
  columns: [], //array of column names
  data: [[]], //2 dimensional array with the data. The outer array holds each row.
  index: [] //index value for each row (partial support)  
}
```

##### Other types
Depending on the language, other types might have built-in serialization. Here is a list of them per language.

> TODO: Turn into a table and add the serialization format

* Python: Pandas Series, Matplotlib Figures
* Scala: none
* R: time series

#### Extend Serialization

##### Python
In Python, custom serializer for unsupported types can be implemented right on the Notebook by subclassing `urth.util.serializers.BaseSerializer`. For example:

```python
class FooSerializer(urth.util.serializers.BaseSerializer):
    @staticmethod
    def klass():
        return Foo

    @staticmethod
    def serialize(obj, **kwargs):
        return obj.foo()

```

##### Scala
In Scala, the only way to support custom types is to implement a `toString` method that returns a JSON String.

##### R
In R, custom serializers for unsupported types can be implemented right on the Notebook by subclassing `Serializer ` and registering the serializer using the exposed `register_serializer` function. For example:

```R
Foo_Serializer <- R6Class(
    'Foo_Serializer',
    inherit = Serializer,
    public = list(
        klass = function() {
            return ("Foo")
        },
        serialize = function(obj) {
            return (obj$foo())
        },
        check_packages = function() {
            tryCatch({
                library(base)
            }, error = function(e) {
                return (False)
            })
            return (True)
        },
        initialize = function() {
            #initialize must exists on this class
        }
    )
)

register_serializer(Foo_Serializer$new())

```
For a more detailed use case of custom R serialization please see our example [urth-r-widgets notebook](https://github.com/jupyter-incubator/declarativewidgets/blob/master/etc/notebooks/examples/urth-r-widgets.ipynb).  Examples of our builtin serializers can be found [here](https://github.com/jupyter-incubator/declarativewidgets/blob/master/kernel-r/declarativewidgets/R/serializers.r).

