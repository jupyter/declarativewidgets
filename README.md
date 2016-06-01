[![Build Status](https://travis-ci.org/jupyter-incubator/declarativewidgets.svg?branch=master)](https://travis-ci.org/jupyter-incubator/declarativewidgets)

# Jupyter Declarative Widget Extension

Extension for Jupyter / IPython Notebook to build interactive areas using
declarative widgets.

## What It Gives You
Watch from minute 21 to 41 of the [September 1st Jupyter meeting video recording](https://www.youtube.com/watch?v=SJiezXPhVv8).

* A base extension that enable the use of [Web Components](http://webcomponents.org) and [Polymer](https://www.polymer-project.org/1.0/) elements
* A set of core elements facilitate interacting with code running on the kernel
* Ability of read and query DataFrames from a variety of implementations (i.e. Spark, R, Pandas).
* Extensions to data binding support and installing of 3rd party elements.
* Implementations for Python, R (using [IRkernel](https://github.com/IRkernel/IRkernel)) and Scala (using [Apache Toree](https://github.com/apache/incubator-toree))

## What It Lacks

* Support for disconnected (no kernel) environments (i.e. nbviewer)

## Runtime Requirements

* Jupyter Notebook 4.0.x, 4.1.x, or 4.2.x running on Python 3.x or Python 2.7.x (see the [0.1.x branch](https://github.com/jupyter-incubator/declarativewidgets/tree/0.1.x) for IPython Notebook 3.2.x compatibility)
* [IPywidgets](https://github.com/ipython/ipywidgets) 4.1.x and 5.1.1+ (R and Scala support not available for 5.0.0 nor 5.1.0)
* Bower - Necessary for installing 3rd party elements straight out of notebook

##### Optional Requirements based on language support
* Apache Toree for access to Spark using Scala
* [IRkernel](https://github.com/IRkernel/IRkernel) for R language

##### Additional requirements for Pandas DataFrame queries
* `pip install numexpr`

##### Additional requirements for Python 2.7
* `pip install futures==3.0.3`

## Try It

We're running a tmpnb instance at [http://jupyter.cloudet.xyz](http://jupyter.cloudet.xyz) with a snapshot of this project (and other related incubator projects) pre-installed.

## Install It

##### In Jupyter 4.2.x

```bash
# install the python package
pip install jupyter_declarativewidgets

# Install all parts of the extension to the active conda / venv / python env
# and enable all parts of it in the jupyter profile in that environment
# See jupyter declarativewidgets quick-setup --help for other options (e.g., --user)
jupyter declarativewidgets quick-setup --sys-prefix
# The above command is equivalent to this sequence of commands:
# jupyter serverextension enable --py declarativewidgets --sys-prefix
# jupyter nbextension install --py declarativewidgets --sys-prefix
# jupyter nbextension enable --py declarativewidgets --sys-prefix
```

##### In Jupyter 4.0.x and 4.1.x

```bash
# install the python package
pip install jupyter_declarativewidgets

# register the notebook frontend extensions into ~/.local/jupyter
# see jupyter cms install --help for other options
jupyter declarativewidgets install --user --symlink --overwrite
# enable the JS and server extensions in your ~/.jupyter
jupyter declarativewidgets activate
```

##### Optional install of R support (all Jupyter versions)

```bash
# installing R support
jupyter declarativewidgets installr --library path/to/r/libs
```

##### Note
On all Jupyter versions, you will need to restart your notebook server if it was running during the enable/activate step. Also, note that you can run jupyter --paths to get a sense of where the extension files will be installed.

## Uninstall It

##### In Jupyter 4.2.x

```bash
# Remove all parts of the extension from the active conda / venv / python env
# See jupyter declarativewidgets quick-remove --help for other options (e.g., --user)
jupyter declarativewidgets quick-remove --sys-prefix
# The above command is equivalent to this sequence of commands:
# jupyter bundler disable --py declarativewidgets --sys-prefix
# jupyter nbextension disable --py declarativewidgets --sys-prefix
# jupyter nbextension uninstall --py declarativewidgets --sys-prefix
# jupyter serverextension disable --py declarativewidgets --sys-prefix

# Remove the python package
pip uninstall jupyter_declarativewidgets
```

##### In Jupyter 4.0.x and 4.1.x

```bash
# Disable extensions, but no way to remove frontend assets in this version
jupyter declarativewidgets deactivate

# Remove the python package
pip uninstall jupyter_declarativewidgets
```

## Documentation

The latest documentation can be found [here](http://jupyter-incubator.github.io/declarativewidgets/docs.html).

Documentation is also available from within the notebook. To see the documentation add a cell with

```html
%%HTML
<urth-help/>
```

## Develop

This repository is setup for a Dockerized development environment. On a Mac, do this one-time setup if you don't have a local Docker environment yet.

```bash
brew update

# make sure we have node and npm for frontend preprocessing
brew install npm node

# make sure you're on Docker >= 1.7
brew install docker-machine docker
docker-machine create -d virtualbox dev
eval "$(docker-machine env dev)"
```

Clone this repository in a local directory that docker can volume mount:

```bash
# make a directory under ~ to put source
mkdir -p ~/projects
cd !$

# clone this repo
git clone https://github.com/jupyter-incubator/declarativewidgets.git
```

Run the notebook server in a docker container:

```bash
cd declarativewidgets

# one time only setup needed to create docker image
make init

# run notebook server in container
make dev
```

The final `make` command starts a local Docker container with the critical pieces of the source tree mounted where they need to be to get picked up by the notebook server in the container. Most code changes on your Mac will have immediate effect within the container.

To see the Jupyter instance with extensions working:

1. Run `docker-machine ls` and note the IP of the dev machine.
2. Visit http://THAT_IP:8888 in your browser

##### Develop Against Python 2.7

You can run a development environment against python 2.7 by adding an environment variable to your make calls.

```bash
# Run a development environment against 2.7
PYTHON=python2 make dev
```

### Build & Package

Run `make sdist` to create a `pip` installable archive file in the `dist` directory. To test the package, run 'make server'. This command will run a docker container and pip install the package. It is useful to validate the packaging and installation. You should be able to install that tarball using pip anywhere you please with one caveat: the setup.py assumes you are installing to profile_default. There's no easy way to determine that you want to install against a different user at pip install time.

### Test

On a Mac, `make test` will execute the browser, python and scala tests.

```
$ make test
Installing and starting Selenium server for local browsers
Selenium server running on port 50625
Web server running on port 2000 and serving from /Users/youruser/declarativewidgets
chrome 45                Beginning tests via http://localhost:2000/generated-index.html?cli_browser_id=0
chrome 45                Tests passed
Test run ended with great success

chrome 45 (102/0/0)

Running python tests...
............................
----------------------------------------------------------------------
Ran 28 tests in 0.006s

Running scala tests...
...
[info] Run completed in 8 seconds, 137 milliseconds.
[info] Total number of tests run: 78
[info] Suites: completed 7, aborted 0
[info] Tests: succeeded 78, failed 0, canceled 0, ignored 0, pending 0
[info] All tests passed.
[success] Total time: 58 s, completed Sep 22, 2015 7:31:12 PM

```

The browser tests are written using the [Web Component Tester](https://github.com/Polymer/web-component-tester) framework. The framework is customized for Polymer and also exposes the following utilities:

* [Mocha](http://mochajs.org/) test framework.
* [Chai](http://chaijs.com/) assertions.
* [Async](https://github.com/caolan/async).
* [Lodash](https://lodash.com/) (3.0)
* [Sinon](http://sinonjs.org/) and [sinon-chai](https://github.com/domenic/sinon-chai)

Tests are located in the `test` directory of each Polymer element in `elements/`.

##### Debugging Web Component Tester Test Failures

Here are some steps that are useful for debugging test failures:

1. Execute web component tester in persistant mode from the root directory:
```bash
make testdev
```
2. Add a `debugger;` line to the test you want to debug.
3. Open a browser window and explicitly load the following url (Note: it is the same url that the test tool uses but all parameters have been removed from the end):

    http://localhost:2000/generated-index.html
4. Open the developer tools for the browser and refresh. The tests should execute and then stop at the `debugger;` line. The code can now be debugged by stepping through it in the developer tools debugger.

##### Testing Against Python 2.7

You can run a tests against python 2.7 by adding an environment variable to your make calls.

```bash
# Run unit tests against 2.7
PYTHON=python2 make test
```

### Element Documentation

Public elements and API are documented using [Polymer suggested guidelines](http://polymerelements.github.io/style-guide/).
Documentation can be run locally with the `make docs` target:

```
$ make docs
Moving static doc site content
Running hydrolysis to generate doc json
Running polybuild on docs.html
...
Serving docs at http://127.0.0.1:4001
```

Load the specified url in your browser to explore the documentation.

## Other Topics

### Initializing the Declarative Widgets extension

Before using the features provided by the Declarative Widgets extension in a
notebook, the extension must first be initialized. The initialization process
loads all dependencies and performs other required environment setup.
Initialization is accomplished through a kernel specific API. Examples for each
of the supported kernels is provided below. The initialization cell must precede
the first cell in the notebook that makes use of Declarative Widgets features.

#### Python Initialization

```
from urth import widgets

widgets.init()
```

#### Scala Initialization

```
// modify to IP and Port of this notebook server
%addjar http://localhost:8888/nbextensions/urth_widgets/urth-widgets.jar

import urth.widgets._

initWidgets
```

#### R Initialization

```
library("declarativewidgets")
initWidgets()
```

### Including a Web Component in a Notebook

The Declarative Widgets framework provides a mechanism to easily install and
import a web component into a notebook. This mechanism is built on top of
[bower packages](http://bower.io/) which are the current standard for publishing
web components. Use the `urth-core-import` element upgraded `link` tag to
include a web component element in a notebook.

```
%%html
<link rel='import' href='urth_components/paper-slider/paper-slider.html'
        is='urth-core-import' package='PolymerElements/paper-slider'>
<paper-slider></paper-slider>
```

The above code will first attempt to load `paper-slider.html`. If that fails,
the specified package will be downloaded and installed on the server with `bower install`. The link `href` will then be requested again to load `paper-slider.html` and the related tag (`paper-slider` in this example) will render as is defined by the element.

To display some minimal details about the package loading in the developer console, specify the `debug` parameter.

### Custom JavaScript API execution

Since Declarative Widgets initialization and import of web components is
performed asynchronously, an elements upgraded JavaScript API may not be
accessible upon execution of the cell. In order to ensure that the required
API is available, make use of `Urth.whenReady(function)`. This API will invoke
the specified function only after prerequisites have been satisfied. The example
code below demonstrates how to safely access the API of the custom
`urth-core-channel` element:

```
%%html
<urth-core-channel name="mine" id="mychannel"></urth-core-channel>
```

```
%%javascript
var channel = document.getElementById('mychannel');

Urth.whenReady(function() {
    channel.set('myvar', 'myvalue');
});
```
