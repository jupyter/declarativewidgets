To visualize static data or data from `urth-core-dataframe`, DeclarativeWidgets provide multiple visualization elements: `urth-viz-table` and a collection of `urth-viz` charts.

To specify what kind of chart, a the `type` attribute may be specified on `urth-viz-chart`, or the specific chart element could be used directly. For example, using `<urth-viz-chart type='bar'></urth-viz-chart>` is the same as using `<urth-viz-bar></urth-viz-bar>`. The chart types supported are:
* area
* bar
* line
* pie
* scatter

## Data Input
Both `urth-viz-table` and `urth-viz-chart` consume data in the same formats. They both have `datarows` and `columns` attributes to receive data.  `datarows` is specified as an Array of Arrays.  It is also valid to pass in a single row element without an enclosing Array.  This makes it easier to declaratively bind the data input to a single selection on another element, as seen in the example, below.

### Static JSON Data
The following shows a simple way to create a bar chart with static JSON data. The same syntax can be applied for `urth-viz-table`.
```
%%html
<urth-viz-bar
    datarows='[["a",8,5],["b",2,6],["c",5,7]]'
    columns='["Index","Series 1","Series 2"]'>
</urth-viz-bar>
```
### From urth-core-dataframe
First, initialize a Pandas Dataframe:
```
aDataFrame1 = pd.DataFrame([
        [1, 10, 5],
        [2, 2, 6],
        [3, 5, 7]
    ], columns=["Index", "Series 1", "Series 2"]
)
```

Then, bind `aDataFrame1` to an `urth-core-dataframe` element, see [Using DataFrames](https://github.com/jupyter-incubator/declarativewidgets/wiki/Using-DataFrames) for more on how to use DataFrames. Now we can make use of this `urth-core-dataframe` to supply datarows and columns for a table visualization.

```
%%html
<template is="dom-bind">
    <urth-core-dataframe ref="aDataFrame1" value="{{df}}" auto>
    </urth-core-dataframe>
    <urth-viz-table datarows="{{df.data}}" columns="{{df.columns}}">
    </urth-viz-table>
</template>
```

## Column Formatting
To specify special formatting for a particular column of data, add a child `urth-viz-col` element.  

For `urth-viz-table`, the `type` and `format` pairs are passed to the underlying [`handsontable implemention`](https://github.com/handsontable/handsontable/wiki/options#column-options).
```
<urth-viz-table datarows="{{ df.data }}" columns="{{ df.columns }}">
        <urth-viz-col index="0" format="$0,0.0" type="numeric"></urth-viz-col>
        <urth-viz-col index="1" format="$0,0.00" type="numeric"></urth-viz-col>
</urth-viz-table>
```

For charts, types of `numeric` and `date` are supported, with format strings [used by `d3`](https://github.com/d3/d3/wiki/Formatting), for example:
```
<urth-viz-chart id="c10" type="line"
   datarows='[["2015-01-01T12:00",8,5],["2015-03-01T12:00",2,6],["2015-04-01T12:00",5,7]]' 
   columns='["Index","Series 1","Series 2"]'>
  <urth-viz-col index="0" type="date" format="%b %d"></urth-viz-col>
  <urth-viz-col index="1" type="numeric" format="$,.2f"></urth-viz-col>
</urth-viz-chart>
```

## Row Selection
Declarative Widgets allow for user interactivity with the table or chart by directly clicking on a row in table, a bar in bar chart, or a slice in pie chart, etc. The corresponding data row is exposed in the property `selection` as an Array, by default, or as key-value pairs if `selection-as-object` is specified.  Additional element-specific data is provided in the `selection-info` attribute, and the data row number is available in the `selection-index` attribute.  By default, both table and chart allow single selection. To turn on multi selection for charts, add attribute `multi-select`.  Where multiple selections are enabled, `selection` will return an Array of rows.