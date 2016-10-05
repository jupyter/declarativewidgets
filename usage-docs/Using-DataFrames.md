DataFrame is a very popular API for data analysis. Python has Pandas DataFrames, Spark has a DataFrame abstraction to big distributed data and R has DataFrames. DeclarativeWidgets provides a way to connect the data held by any of these DataFrames implementations to interactive visual elements in the Notebook.

The `urth-core-dataframe` element brings a representation of an actual DataFrame into the HTML template. It allows other elements in the template to visualize the data held by the DataFrame.

#### Exploring the DataFrame

```python
from declarativewidgets import explore
explore(df, channel='default', properties={'selection-as-object: True'}, bindings={'selection': 'the_selection')
```
The `explore` function enables visual exploration of different types of DataFrames across all kernels (Python, Scala, and R)

| Parameter    	| Description  |
|---------------	|--------------|
| `df`        	| String name or actual reference to a DataFrame|
| `channel`   	| specifies the channel name to use in the generated `template`|
| `properties`	| key-value pairs where the key is a property of `<urth-viz-explorer>` and the value is a literal value to assign to the property|
| `bindings`   	| key-value pairs where the key is a property of `<urth-viz-explorer>` and the value is interpreted as the name of a binding variable|

> For more information see [declarativewidgets_explorer](https://github.com/jupyter-incubator/declarativewidgets_explorer)

![](https://raw.githubusercontent.com/jupyter-incubator/declarativewidgets_explorer/master/media/explorer_screencast.gif)

#### Connecting the element to the DataFrame

The `urth-core-dataframe` elements connects to a DataFrame in the kernel by name using the `ref` property. 

#### Getting the content of the DataFrame

Once the element finds the DataFrame in the kernel, it get the data and makes it available in several ways.

* The `value` property has an Object representation of the DataFrame.

	```javascript
{
   columns: [], //array of column names
   columnTypes: [], //array of column type names
   data: [[]], //2 dimensional array with the data. The outer array holds each row.
   index: [] //index value for each row (partial support)  
}
	```

* The `rows` property has only the data portion of the DataFrame and can be structured as a 2D Array or an Array of Objects. This is control by setting property `rowAsObject`.
  
	```javascript
[
	[the, row, data],
	[the, row, data],
	[the, row, data]
]  
OR
[
	{
		col1: the
		col2: row
		col3: data
	},
	{
		col1: the
		col2: row
		col3: data
	}
]  
	```

* The `columns` property has an Array of column names

* The `columnTypes` property has an Array of column type names

Note: Column types from the native kernel language to the client are resolved/mapped according to the table below:

JS objects| Pandas | pySpark |R data.frame| Spark R| Spark Scala|
| ------------- | ------------- | ------------- | ------------- | ------------- | ------------- |
Number | int64 | bigint | integer | integer | Int |
Number | float64 | double | numeric | numeric | Double |
Boolean | bool | boolean | logical | logical | Boolean
String | [object (object of ndarray of strings)](http://stackoverflow.com/questions/21018654/strings-in-a-dataframe-but-dtype-is-object) *See type of Unknown | string | character | character | String
Date | datetime64[ns] | date | Date | Date | TimestampType, DateType|
Unknown | object or ambiguous type | object or ambiguous type | object or ambiguous type | object or ambiguous type |object or ambiguous type|

All property can be used in data bindings to render the data. The example above shows the data displayed as a set of cards, but the same data can be visualize using many of the `<urth-viz-*>` elements.

#### Updates to the data

`urth-core-dataframe` can be configured to receive updates in the case that the content of the DataFrame changes due to code executing on the kernel. Use the `auto` property to turn on automatic updates.

#### Querying the DataFrame 

As of version `0.6.0` of DeclarativeWidgets, the `urth-core-dataframe` element has experimental support for declaring queries for the DataFrame that can be modified by other visual elements on the Notebook. 

> TODO: Code or Image of example here

The queries are defined by placing the following element within an `urth-core-dataframe`:

* `urth-core-query-filter` allows for filter expressions as text with `{{binding}}` variables.
* `urth-core-query-group` allows for grouping and aggregating the data.
* `urth-core-query-sort` allows for sorting the data.


For more detail information about the `urth-core-dataframe` element, see the [api docs](http://jupyter-incubator.github.io/declarativewidgets/docs.html). Also visit the specific api documentation for each of the query elements.