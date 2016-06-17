In DeclarativeWidgets, building interactive areas in the Jupyter Notebook is a combination of 
* Initialization
* HTML markup
* [Polymer](https://www.polymer-project.org/1.0/) techniques for data binding
* DeclarativeWidgets elements to visualize and connect to data and functions
* 3rd party elements such as those in the [Polymer catalog](https://elements.polymer-project.org/)
* Code and data in the kernel (Python, Scala, or R)

#### Initializing the Declarative Widgets extension

Before using the features provided by the Declarative Widgets extension in a
notebook, the extension must first be initialized. The initialization process
loads all dependencies and performs other required environment setup.
Initialization is accomplished through a kernel specific API. Examples for each
of the supported kernels is provided below. The initialization cell must precede
the first cell in the notebook that makes use of Declarative Widgets features.

##### Python Initialization

```
import declarativewidgets as declwidgets

declwidgets.init()
```

##### Scala Initialization

```
// modify to IP and Port of this notebook server
%addjar http://localhost:8888/nbextensions/declarativewidgets/declarativewidgets.jar

import declarativewidgets._

initWidgets
```

##### R Initialization

```
library("declarativewidgets")
initWidgets()
```

#### HTML Markup
In DeclarativeWidgets, interactive areas of the Jupyter Notebook are authored primarily through HTML. In Python and Toree kernels, the HTML can easily be done by using the `%%HTML` magic in a code cell.

> TODO: Image of %%HTML example

In IRkernel, there is no magic syntax support, so the HTML is send as a string to the function `IRdisplay::display_html`.

> TODO: Image of IRdisplay::display_html.

#### Templates and data binding
DeclarativeWidgets is built using [Polymer](https://www.polymer-project.org/1.0/), so a typical HTML fragment combines a number of elements (native HTML or [Web Components](ç)) connected using data bindings within a `<template>` element.  

> TODO: Image of a simple template

The `<template>` element provides a context for the containing elements to share data using the `{{data}}` syntax. This syntactical sugar is known as a data binding and it's what powers the interactions between the user, the elements, and the code in the kernel. For more details about how to build `templates`, data binding and other Polymer utilities, refer to the Polymer [user guide](https://www.polymer-project.org/1.0/docs/devguide/feature-overview.html).

#### DeclarativeWidgets elements

DeclarativeWidgets provide a collection of elements and utilities that enable access to data and functions defined in the kernel. The combination of these elements with other native HTML or Polymer elements, create very powerful and rich interactive areas. More details on what can be done with some of the DeclarativeWidgets elements in the following topics.

#### 3rd party elements

One advantage of using Web Components and Polymer, is that the user can tap into the ecosystem of 3rd party elements and make those part of the Notebook. The Declarative Widgets framework provides a mechanism to easily install and import a web component into a notebook. With the following snippet of HTML in a cell, the developer can download and install new components and then use them in the Notebook.

```
%%html
<link rel='import' href='urth_components/paper-slider/paper-slider.html'
        is='urth-core-import' package='PolymerElements/paper-slider'>
<paper-slider></paper-slider>
```

The above code will first attempt to load `paper-slider.html`. If that fails,
the specified `package` will be used with [bower](http://bower.io/) to download and install the web component on the server. The link `href` will then be requested again to load `paper-slider.html` and the related tag (`paper-slider` in this example) will render as is defined by the element.

Any web component that can be installed by `bower install` may be included in a notebook through this mechanism. In many of the examples, we leverage the collection of elements in the [Polymer catalog](https://elements.polymer-project.org/).

#### Functions and data

The last piece is the code that runs in the kernel. Functions can be used to load data or handle events from the visual elements. The data from DataFrames created in Pandas, Spark and others can be shared to visualize in the Notebook.