# Changelog
## 0.6.1 (2016-07-01)
* Fix to issue when Run All and using channel.set
* Minor fixes to declarative queries

## 0.6.0 (2016-06-17)
* User must manually activate declarativewidgets using a code cell
* Declarative query support for dataframes
* Usage documentation
* Deprecation of `urth.widgets` module. Use `declarativewidgets` instead

## 0.5.3 (2016-06-08)
* Fixes to timing error related to loading order of this extension in relation to ipywidgets/jupyter-js-widgets
* Fixes to support running Windows
* Added selection-info on `<urth-viz-table>` to get cell coordinates

## 0.5.2 (2016-05-03)
* Support for selectionIndex in urth-viz-*
* Custom serializers in R

## 0.5.1 (2016-04-29)
* Support for ipywidgets 5.1.1+
* Fixes to support alternate non-notebook runtimes

## 0.5.0 (2016-27-16)
* Support for Jupyter 4.2 and ipywidgets 5.0
* Support for R language through IRkernel
* `selection` property on charts references row data. Chart-specific selection data available as `selectionInfo`
* `selection` property on charts references a single row rather than Array of length==1, continues to return Array of rows when `multiSelect` is used.
* New element <urth-help> to display help documentation inline
* New element <urth-viz-ipywidget> to embed an ipywidget in a declarative template
* Better support for dependency injection when starting up declarativewidgets

## 0.4.5 (2016-04-06)
* Progress indications on templates while dependencies load

## 0.4.4 (2016-04-01)
* Fix regression in selection in charts

## 0.4.3 (2016-03-24)
* Ability to inspect the content of an urth-core-channel
* Improved selection content from urth-viz-chart elements
* Misc bug fixes on urth-viz-chart
* Ability to peg Polymer version at runtime [1.2.4-1.4.0)
* Fixes to urth-core-function to better support complex parameters
* Serialization fixes for Toree/Scala

## 0.4.2 (2016-03-03)
* Make selection in urth-viz-table available as an object
* Allow height, width, and margin adjustment to urth-viz-chart's

## 0.4.1 (2016-02-23)
* Support for Polymer 1.3
* Fixes to complex object changes across templates
* Delay template rendering until imports completed

## 0.4.0 (2016-02-12)
* Upgrade to use Apache Toree for scala kernel
* Upgrade to Polymer 1.2.4+

## 0.3.2 (2016-03-22)
* Ability to peg Polymer version at runtime `[1.2.4-1.4.0)`

## 0.3.1 (2016-02-08)

* Support for Polymer 1.2.4+

## 0.3.0 (2016-01-20)

* Modified install process to rely on `jupyter activate/deactivate`
* New element `urth-core-channel` and javascript API to channel variables
* New element `urth-core-watch`
* Fixes to sizing of 'urth-viz-table'
* Serialization support for Pandas.Series

## 0.2.0 (2015-12-01)

* Make compatible with Jupyter Notebook 4.0.x
* Several fixes to `urth-viz-table` around selection
* Support for `display_data` type messages sent when a function is invoked through `urth-core-function`
* Added support to label axis on `urth-viz-chart` elements

## 0.1.4 (2016-03-22)
* Ability to peg Polymer version at runtime

## 0.1.3 (2016-02-16)
* Upgrade to Polymer 1.2.4
* Allow graceful degradation when server extensions are not available

## 0.1.2 (2016-01-18)
* Backport fixes from master

## 0.1.1 (2015-12-01)

* Backport of UX fixes from 0.2.0
* Keep compatible with IPython Notebook 3.2.x

## 0.1.0 (2015-11-17)

* First PyPI release
* Compatible with IPython Notebook 3.2.x on Python 2.7 or Python 3.3+