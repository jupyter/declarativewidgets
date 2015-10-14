[![Build Status](https://travis-ci.org/jupyter-incubator/declarativewidgets.svg)](https://travis-ci.org/jupyter-incubator/declarativewidgets)

# Jupyter Declarative Widget Extension

Extension for Jupyter / IPython Notebook to build interactive areas using 
declarative widgets.

## What It Gives You
Watch from minute 21 to 41 of the [September 1st Jupyter meeting video recording](https://www.youtube.com/watch?v=SJiezXPhVv8). 

* A base extension that enable the use of [Web Components](http://webcomponents.org) and [Polymer](https://www.polymer-project.org/1.0/) elements
* A set of core elements facilitate interacting with code running on the kernel
* Extensions to data binding support and installing of 3rd party elements.
* Implementations for Python Kernel and [Spark Kernel](https://github.com/ibm-et/spark-kernel)

## What It Lacks

* Support for state persistance and downstream tools (nbviewer)
* Interactions with DataFrames. Currently read-only.
* Better error handling
* More elements and support for other kernels.

## Runtime Requirements

* IPython Notebook 3.2.x (not Jupyter Notebook 4.x, yet) running on Python 3.x
* Bower - Necessary for installing 3rd party elements straight out of notebook
* Spark Kernel if wanting to run Spark using Scala

Note: These are satisfied automatically when you follow the setup instructions below.

## Try It

We're running a tmpnb instance at [http://jupyter.cloudet.xyz](http://jupyter.cloudet.xyz) with a snapshot of this project (and other related incubator projects) pre-installed.

## Develop

This repository is setup for a Dockerized development environment. On a Mac, do this one-time setup if you don't have a local Docker environment yet.

```
brew update

# make sure we have node and npm for frontend preprocessing
brew install npm node

# make sure you're on Docker >= 1.7
brew install docker-machine docker
docker-machine create -d virtualbox dev
eval "$(docker-machine env dev)"
```

Pull the Docker image that we'll use for development (super image with bower and spark kernel). This step is optional, `make dev` will bring in all requirements, including images.

```
docker pull cloudet/pyspark-notebook-bower-sparkkernel
```

Clone this repository in a local directory that docker can volume mount:

```
# make a directory under ~ to put source
mkdir -p ~/projects
cd !$

# clone this repo
git clone https://github.com/jupyter-incubator/declarativewidgets.git
```

Run the notebook server in a docker container:

```
# run notebook server in container
cd declarativewidgets
make dev
```

The final `make` command starts a local Docker container with the critical pieces of the source tree mounted where they need to be to get picked up by the notebook server in the container. Most code changes on your Mac will have immediate effect within the container.

To see the Jupyter instance with extensions working:

1. Run `docker-machine ls` and note the IP of the dev machine.
2. Visit http://THAT_IP:8888 in your browser

## Build & Package

Run `make sdist` to create a `pip` installable archive file in the `dist` directory. To test the package, run 'make server'. This command will run a docker container and pip install the package. It is usefull to validate the packaing and installation.

## Install

Ensure you have the following prerequisites met:
* IPython Notebook 3.2.x (no Jupyter Notebook 4.x-pre yet)
* Notebook instance running out of `profile_default`
* Make sure you installing and running on a Python3 environment. (i.e. python, ipython, and pip)

> Note: <br>
> You should be able to install that tarball using pip anywhere you please with one caveat: the setup.py assumes you are installing to profile_default. There's no easy way to determine that you want to install against a different user at pip install time.


## Test

On a Mac, `make test` will execute the browser, python and scala tests.

```
$ make test
Installing and starting Selenium server for local browsers
Selenium server running on port 50625
Web server running on port 2000 and serving from /Users/drewwalt/Work/Urth/widgets
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

### Debugging Web Component Tester Test Failures

Here are some steps that are useful for debugging test failures:

1. Execute web component tester in persistant mode from the root directory:
```
make testdev
```
2. Add a `debugger;` line to the test you want to debug.
3. Open a browser window and explicitly load the following url (Note: it is the same url that the test tool uses but all parameters have been removed from the end):

    http://localhost:2000/generated-index.html
4. Open the developer tools for the browser and refresh. The tests should execute and then stop at the `debugger;` line. The code can now be debugged by stepping through it in the developer tools debugger.

## Documentation

Public elements and API are documented using [Polymer suggested guidelines](http://polymerelements.github.io/style-guide/).
Documentation can be run locally with the `make docs` target:

```
$ make docs
urth_widgets_docs
Sending build context to Docker daemon  7.68 kB
Sending build context to Docker daemon
...
Documentation available at <ip_address>:4001/components/urth-widgets/
```

Documentation is run inside of a docker container. Load the specified url in your
browser (substitute &lt;ip_address&gt; with your docker host ip) to explore the documentation.

### Including a Web Component in a Notebook

The Urth widgets framework provides a mechanism to easily install and import a web component into
a notebook. This mechanism is built on top of [bower packages](http://bower.io/) which are the
current standard for publishing web components. Use the `urth-core-import` element upgraded `link` tag to include a web component element in a notebook.

```
%%html
<link rel='import' href='urth_components/paper-slider/paper-slider.html'
        is='urth-core-import' package='PolymerElements/paper-slider'>
<paper-slider></paper-slider>
```

The above code will first attempt to load `paper-slider.html`. If that fails,
the specified package will be downloaded and installed on the server with `bower install`. The link `href` will then be requested again to load `paper-slider.html` and the related tag (`paper-slider` in this example) will render as is defined by the element.

To display some minimal details about the package loading in the developer console, specify the `debug` parameter.
