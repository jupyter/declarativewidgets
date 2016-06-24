# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.

.PHONY: help init clean clean-dist clean-watch clean-watch-docs dist sdist docs
.PHONY: dev dev_image dev_image_4.2 _dev _dev-python2 _dev-python3
.PHONY: server server_4.2 remove-server install install-all all release
.PHONY: start-selenium stop-selenium _run _run-python3 _run-python2 run-test
.PHONY: test testdev test-js test-js-remote test-py test-py-all _test-py
.PHONY: _test-py-python2 _test-py-python3 test-scala test-r
.PHONY: system-test system-test-all system-test-all-local system-test-all-remote
.PHONY: system-test-python3 system-test-python2 system-test-alt-jupyter
.SUFFIXES:
MAKEFLAGS=-r

help:
	@echo '            init - setups machine with base requirements for dev'
	@echo '           clean - clean build files'
	@echo '     clean-watch - tries to stop the file watch started by dev'
	@echo 'clean-watch-docs - tries to stop the file watch started by docs'
	@echo '             dev - start container with source mounted for development'
	@echo '            docs - start container that serves documentation'
	@echo '           sdist - build a source distribution'
	@echo '         install - install latest sdist into a container'
	@echo '          server - starts a container with extension installed through pip'
	@echo '     system-test - run system integration tests with selenium'
	@echo '            test - run all units'
	@echo '         test-py - run python units'
	@echo '         test-js - run javascript units'
	@echo '      test-scala - run scala units'
	@echo '      test-r     - run r units'
	@echo '             all - run all necessary streps to produce and validate a build'

# Docker images and repos
ROOT_REPO:=jupyter/all-spark-notebook:2d878db5cbff
REPO:=jupyter/all-spark-notebook-bower:2d878db5cbff
REPO4.2:=jupyter/all-spark-notebook-bower-jup4.2:2d878db5cbff
SCALA_BUILD_REPO:=1science/sbt

# Global environment defaults
PORT_MAP?=-p 9500:8888
BROWSER_LIST?=chrome
ALT_BROWSER_LIST?=chrome
BASEURL?=http://192.168.99.100:9500
TEST_TYPE?=local
SPECS?=system-test/urth-core-bind-specs.js system-test/urth-system-test-specs.js system-test/urth-viz-table-specs.js
PYTHON2_SPECS?=system-test/urth-system-test-specs.js
ALT_JUPYTER_SPECS?=system-test/urth-system-test-specs.js
ALT_JUPYTER_VERSION?=4.2
PYTHON?=python3
TEST_MSG?="Starting system tests"

# Logging levels
DOCKER_OPTS?=--log-level warn
PIP_OPTS?=--quiet
BOWER_OPTS?=--quiet

URTH_BOWER_FILES:=$(shell find elements -name bower.json)
URTH_SRC_DIRS:=$(foreach dir, $(URTH_BOWER_FILES), $(shell dirname $(dir)))
URTH_DIRS:=$(foreach dir, $(URTH_SRC_DIRS), $(shell basename $(dir)))
URTH_COMP_LINKS:=$(foreach dir, $(URTH_DIRS), $(shell echo "bower_components/$(dir)"))
NPM_BIN_DIR:=$(shell npm bin)
$(URTH_COMP_LINKS): | node_modules/bower $(URTH_SRC_DIRS)
	@echo 'Linking local Urth elements'
	@$(foreach dir, $(URTH_SRC_DIRS), cd $(abspath $(dir)) && $(NPM_BIN_DIR)/bower link $(BOWER_OPTS);)
	@$(foreach name, $(URTH_DIRS), $(NPM_BIN_DIR)/bower link $(name) $(BOWER_OPTS);)

init: node_modules dev_image dev_image_4.2

node_modules: package.json
	@npm install --quiet

node_modules/bower: node_modules

bower_components: node_modules/bower bower.json
	@npm run bower -- install $(BOWER_OPTS)

dev_image:
	@-docker $(DOCKER_OPTS) rm -f bower-build
	@docker $(DOCKER_OPTS) run -it --user root --name bower-build \
		-v `pwd`/etc/r/install.r:/src-kernel-r/install.r \
		$(ROOT_REPO) bash -c 'apt-get -qq update && \
		apt-get -qq install --yes curl && \
		curl --silent --location https://deb.nodesource.com/setup_0.12 | sudo bash - && \
		apt-get -qq install --yes nodejs npm && \
		ln -s /usr/bin/nodejs /usr/bin/node && \
		npm install -g bower && \
		Rscript /src-kernel-r/install.r && \
		mkdir -p /home/jovyan/.local/share/jupyter/nbextensions && \
		chown -R jovyan:users /home/jovyan/.local/share/jupyter/nbextensions'
	@docker $(DOCKER_OPTS) commit bower-build $(REPO)
	@-docker $(DOCKER_OPTS) rm -f bower-build

dev_image_4.2:
	@-docker $(DOCKER_OPTS) rm -f 4.2-build
	@docker $(DOCKER_OPTS) run -it --user root --name 4.2-build \
		$(REPO) bash -c 'pip uninstall --yes ipywidgets && \
    pip install --upgrade notebook==4.2.0 $(PIP_OPTS) && \
    pip install ipywidgets==5.1.5 $(PIP_OPTS) && \
    jupyter nbextension enable --system --py widgetsnbextension && \
    pip install --pre --upgrade toree $(PIP_OPTS) && \
    jupyter toree install'
	@docker $(DOCKER_OPTS) commit 4.2-build $(REPO4.2)
	@-docker $(DOCKER_OPTS) rm -f 4.2-build

clean: clean-dist
	@-rm -rf *.egg-info
	@-rm -rf __pycache__ */__pycache__ */*/__pycache__
	@-find . -name '*.pyc' -exec rm -fv {} \;
	@-rm -rf bower_components node_modules

clean-dist:
	@-rm -rf dist

.watch: node_modules
	@echo 'Doing watch'
	@npm run watch & echo $$! > .watch

.watch-docs: node_modules
	@echo 'Doing watch-docs'
	@npm run watch-docs & echo $$! > .watch-docs

clean-watch:
	@echo 'Killing watch'
	-@kill -9 `pgrep -P $$(cat .watch)`
	-@rm .watch

clean-watch-docs:
	@echo 'Killing watch-docs'
	-@kill -9 `pgrep -P $$(cat .watch-docs)`
	-@rm .watch-docs

dist/urth/widgets/ext: ${shell find nb-extension/python/urth/widgets/ext}
	@echo 'Moving frontend extension code'
	@mkdir -p dist/urth/widgets/ext
	@cp -R nb-extension/python/urth/widgets/ext/* dist/urth/widgets/ext/.

dist/urth: ${shell find kernel-python/urth} dist/urth/widgets/ext
	@echo 'Moving python code'
	@mkdir -p dist/urth
	@cp -R kernel-python/urth/* dist/urth/.

dist/declarativewidgets: dist/declarativewidgets/static ${shell find nb-extension/python/declarativewidgets} ${shell find kernel-python/declarativewidgets}
	@mkdir -p dist/declarativewidgets
	@echo 'Building declarativewidgets python module'
	@cp -R nb-extension/python/declarativewidgets/* dist/declarativewidgets/.
	@cp -R kernel-python/declarativewidgets/* dist/declarativewidgets/.
	@cat nb-extension/python/declarativewidgets/__init__.py > dist/declarativewidgets/__init__.py
	@echo '\n\n' >> dist/declarativewidgets/__init__.py
	@cat kernel-python/declarativewidgets/__init__.py >> dist/declarativewidgets/__init__.py

dist/declarativewidgets/static: bower.json dist/declarativewidgets/static/css dist/declarativewidgets/static/docs dist/declarativewidgets/static/elements dist/declarativewidgets/static/js dist/declarativewidgets/static/urth_components dist/declarativewidgets/static/declarativewidgets.jar dist/declarativewidgets/static/urth-widgets.tgz
	@cp bower.json dist/declarativewidgets/static/bower.json
	@touch dist/declarativewidgets/static

dist/declarativewidgets/static/css: ${shell find nb-extension/css}
	@echo 'Building declarativewidgets/static/css'
	@mkdir -p dist/declarativewidgets/static/css
	@cp -R nb-extension/css/* dist/declarativewidgets/static/css/.

dist/declarativewidgets/static/docs: dist/docs
	@echo 'Building declarativewidgets/static/docs'
	@mkdir -p dist/declarativewidgets/static/docs
	@cp -R dist/docs/site/* dist/declarativewidgets/static/docs/.

dist/declarativewidgets/static/elements: ${shell find elements}
	@echo 'Building declarativewidgets/static/elements'
	@mkdir -p dist/declarativewidgets/static/elements
	@cp -R elements/* dist/declarativewidgets/static/elements/.
	@touch dist/declarativewidgets/static/elements

dist/declarativewidgets/static/js: ${shell find nb-extension/js}
	@echo 'Building declarativewidgets/static/js'
	@mkdir -p dist/declarativewidgets/static/js
	@cp -R nb-extension/js/* dist/declarativewidgets/static/js/.

dist/declarativewidgets/static/urth_components: bower_components ${shell find elements} | $(URTH_COMP_LINKS)
	@echo 'Building declarativewidgets/static/urth_components'
	@mkdir -p dist/declarativewidgets/static/urth_components
	@cp -RL bower_components/* dist/declarativewidgets/static/urth_components/.
	@touch dist/declarativewidgets/static/urth_components

dist/declarativewidgets/static/declarativewidgets.jar: ${shell find kernel-scala/src/main/scala/}
ifeq ($(NOSCALA), true)
	@echo 'Skipping scala code'
else
	@echo 'Building scala code'
	@echo 'Building declarativewidgets/static/declarativewidgets.jar'
	@mkdir -p dist/declarativewidgets/static
	@docker $(DOCKER_OPTS) run -it --rm \
		-v `pwd`:/src \
		-v `pwd`/etc/ivy:/root/.ivy2 \
		$(SCALA_BUILD_REPO) bash -c 'cp -r /src/kernel-scala/* /app/. && \
			sbt --warn package && \
			cp target/scala-2.10/declarativewidgets*.jar /src/dist/declarativewidgets/static/declarativewidgets.jar && \
			cp target/scala-2.10/declarativewidgets*.jar /src/dist/declarativewidgets/static/urth-widgets.jar'
endif

dist/declarativewidgets/static/urth-widgets.tgz: ${shell find kernel-r/declarativewidgets}
ifeq ($(NOR), true)
	@echo 'Skipping R code'
else
	@echo 'Building R code'
	@echo 'Building declarativewidgets/static/urth-widgets.tgz'
	@mkdir -p dist/declarativewidgets/static
	@docker $(DOCKER_OPTS) run -it --rm \
		-v `pwd`:/src \
		$(REPO) bash -c 'cp -r /src/kernel-r/declarativewidgets /tmp/src && \
			cd /tmp/src && \
			R --quiet CMD INSTALL --build . && \
			cp declarativewidgets_0.1_R_x86_64-pc-linux-gnu.tar.gz /src/dist/declarativewidgets/static/urth-widgets.tgz'
endif

dist/docs: dist/docs/bower_components dist/docs/site dist/docs/site/generated_docs.json

dist/docs/bower_components: node_modules etc/docs/bower.json
	@echo 'Installing documentation dependencies'
	@mkdir -p dist/docs
	@cp etc/docs/bower.json dist/docs/bower.json
	@npm run docsbower -- install $(BOWER_OPTS)

dist/docs/site: node_modules ${shell find etc/docs/site}
	@echo 'Moving static doc site content'
	@mkdir -p dist/docs/site
	@cp -R etc/docs/site/* dist/docs/site
	@echo 'Running polybuild on docs.html'
	@npm run polybuild -- --maximum-crush dist/docs/site/docs.html
	@mv dist/docs/site/docs.build.html dist/docs/site/docs.html

dist/docs/site/generated_docs.json: dist/docs/site bower_components ${shell find elements/**/*.html} etc/docs/hydrolyze_elements.js etc/docs/urth-elements.html | $(URTH_COMP_LINKS)
	@echo 'Running hydrolysis to generate doc json'
	@node etc/docs/hydrolyze_elements.js 'etc/docs/urth-elements.html' 'dist/docs/site/generated_docs.json'

dist/scripts: etc/scripts/jupyter-declarativewidgets
	@mkdir -p dist/scripts
	@cp etc/scripts/jupyter-declarativewidgets dist/scripts/jupyter-declarativewidgets

dist/VERSION: COMMIT=$(shell git rev-parse --short=12 --verify HEAD)
dist/VERSION:
	@mkdir -p dist
	@echo "$(COMMIT)" > dist/VERSION

dist: dist/urth dist/declarativewidgets dist/scripts dist/VERSION

sdist: dist
	@cp -R MANIFEST.in dist/.
	@cp -R setup.py dist/.
	@docker $(DOCKER_OPTS) run -it --rm \
		-v `pwd`/dist:/src \
		$(EXTRA_OPTIONS) \
		$(REPO) bash -c '$(PRE_SDIST) cp -r /src /tmp/src && \
			cd /tmp/src && \
			python setup.py -q sdist $(POST_SDIST) && \
			cp -r dist/*.tar.gz /src/.'

test: test-js test-py test-scala test-r

test-js: BROWSER?=chrome
test-js: | $(URTH_COMP_LINKS)
	@echo 'Running web component tests...'
	@npm run test -- --local $(BROWSER)

test-js-remote: | $(URTH_COMP_LINKS)
ifdef SAUCE_USER_NAME
	@echo 'Running web component tests remotely on Sauce Labs...'
	@npm run test-sauce --silent -- --sauce-tunnel-id \"$(TRAVIS_JOB_NUMBER)\" --sauce-username $(SAUCE_USER_NAME) --sauce-access-key $(SAUCE_ACCESS_KEY)
else
	@npm run test -- --local firefox
endif

test-py: dist/urth dist/declarativewidgets
	@echo 'Running python tests in $(PYTHON)...'
	@$(MAKE) _test-py-$(PYTHON)

_test-py-python2: EXTENSION_DIR=/opt/conda/envs/python2/lib/python2.7/site-packages
_test-py-python2: CMD=python --version; python -m unittest discover $(EXTENSION_DIR)/declarativewidgets "test*[!_py3].py"
_test-py-python2: PYTHON_SETUP_CMD=source activate python2; pip install -U mock $(PIP_OPTS);
_test-py-python2: _test-py

_test-py-python3: EXTENSION_DIR=/usr/local/lib/python3.4/dist-packages
_test-py-python3: CMD=python --version; python -m unittest discover $(EXTENSION_DIR)
_test-py-python3: _test-py

_test-py:
	@docker $(DOCKER_OPTS) run -it --rm \
		-v `pwd`/dist/declarativewidgets:$(EXTENSION_DIR)/declarativewidgets \
		$(REPO) bash -c '$(PYTHON_SETUP_CMD) $(CMD)'

test-py-all:
	@$(MAKE) test-py
	@PYTHON="python2" $(MAKE) test-py

test-scala:
ifeq ($(NOSCALA), true)
	@echo 'Skipping scala tests...'
else
	@echo 'Running scala tests...'
	@docker $(DOCKER_OPTS) run -it --rm \
		-v `pwd`/kernel-scala:/src \
		-v `pwd`/etc/ivy:/root/.ivy2 \
		$(SCALA_BUILD_REPO) bash -c 'cp -r /src/* /app/. && \
			sbt --warn test'
endif

test-r:
ifeq ($(NOR), true)
	@echo 'Skipping R tests...'
else
	@echo 'Running R tests'
	#See http://askubuntu.com/questions/575505/glibcxx-3-4-20-not-found-how-to-fix-this-error
	#for the unlinking/linking issue of a conda outdated lib
	@docker $(DOCKER_OPTS) run -it --rm \
		-v `pwd`/kernel-r/declarativewidgets:/src-kernel-r/declarativewidgets \
		-v `pwd`/etc/r/install_prereq.r:/src-kernel-r/install_prereq.r \
		$(REPO) bash -c 'R --quiet CMD build /src-kernel-r/declarativewidgets && \
		Rscript /src-kernel-r/install_prereq.r && \
		unlink /opt/conda/lib/libstdc++.so.6 && \
		ln -s /usr/lib/x86_64-linux-gnu/libstdc++.so.6.0.20 /opt/conda/lib/libstdc++.so.6 && \
		R CMD check declarativewidgets_0.1.tar.gz'
endif

testdev: BROWSER?=chrome
testdev: | $(URTH_COMP_LINKS)
	@npm run test -- -p --local $(BROWSER)

install: CMD?=exit
install: SERVER_NAME?=urth_widgets_install_validation
install: OPTIONS?=-it --rm
install: _run-$(PYTHON)

install-all:
	@$(MAKE) install
	@PYTHON="python2" $(MAKE) install

server: CMD?=jupyter notebook --no-browser --port 8888 --ip="*"
server: INSTALL_DECLWID_CMD?=pip install $(PIP_OPTS) --no-binary ::all: $$(ls -1 /src/dist/*.tar.gz | tail -n 1) && jupyter declarativewidgets install --user && jupyter declarativewidgets installr --library=/opt/conda/lib/R/library && jupyter declarativewidgets activate;
server: SERVER_NAME?=urth_widgets_server
server: OPTIONS?=-it --rm
server: VOL_MAP?=-v `pwd`/etc/notebooks:/home/jovyan/work
server: _run-$(PYTHON)

server_4.2: CMD?=jupyter notebook --no-browser --port 8888 --ip="*"
server_4.2: INSTALL_DECLWID_CMD?=pip install $(PIP_OPTS) --no-binary ::all: $$(ls -1 /src/dist/*.tar.gz | tail -n 1) && jupyter declarativewidgets quick-setup --user && jupyter declarativewidgets installr --library=/opt/conda/lib/R/library;
server_4.2: SERVER_NAME?=urth_widgets_server
server_4.2: OPTIONS?=-it --rm
server_4.2: VOL_MAP?=-v `pwd`/etc/notebooks:/home/jovyan/work
server_4.2: REPO=$(REPO4.2)
server_4.2: _run-$(PYTHON)

remove-server:
	-@docker $(DOCKER_OPTS) rm -f $(SERVER_NAME)

_run-python3: _run

_run-python2: PYTHON_SETUP_CMD=source activate python2; pip install $(PIP_OPTS) futures==3.0.3;
_run-python2: _run

_run:
	@echo 'Running container named $(SERVER_NAME) in $(PYTHON)'
	@docker $(DOCKER_OPTS) run $(OPTIONS) --name $(SERVER_NAME) \
		$(PORT_MAP) \
		-e SPARK_OPTS="--master=local[4]" \
		-e USE_HTTP=1 \
		-v `pwd`:/src \
		--user jovyan \
		$(VOL_MAP) \
		$(REPO) bash -c '$(PYTHON_SETUP_CMD) $(INSTALL_DECLWID_CMD) $(CMD)'

dev: CMD?=sh -c "python --version; jupyter notebook --no-browser --port 8888 --ip='*'"
dev: _dev-$(PYTHON)

_dev-python2: EXTENSION_DIR=/opt/conda/envs/python2/lib/python2.7/site-packages
_dev-python2: PYTHON_SETUP_CMD=source activate python2; pip install $(PIP_OPTS) futures==3.0.3;
_dev-python2: _dev

_dev-python3: EXTENSION_DIR=/opt/conda/lib/python3.5/site-packages
_dev-python3: _dev

_dev: NB_HOME?=/home/jovyan
_dev: .watch dist
	-@docker $(DOCKER_OPTS) run -it --rm \
		-p 8888:8888 \
		-p 4040:4040 \
		-p 5005:5005 \
		--user jovyan \
		-e SPARK_OPTS="--master=local[4] --driver-java-options=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005" \
		-v `pwd`/dist/declarativewidgets/static:$(NB_HOME)/.local/share/jupyter/nbextensions/declarativewidgets \
		-v `pwd`/dist/declarativewidgets:$(EXTENSION_DIR)/declarativewidgets \
		-v `pwd`/dist/urth:$(EXTENSION_DIR)/urth \
		-v `pwd`/etc:$(NB_HOME)/nbconfig \
		-v `pwd`/etc/notebook.json:$(NB_HOME)/.jupyter/nbconfig/notebook.json \
		-v `pwd`/etc/jupyter_notebook_config.py:$(NB_HOME)/.jupyter/jupyter_notebook_config.py \
		-v `pwd`/etc/notebooks:/home/jovyan/work \
		-v `pwd`/kernel-r/declarativewidgets:/src-kernel-r/declarativewidgets \
		$(REPO) bash -c 'R CMD INSTALL -l /opt/conda/lib/R/library /src-kernel-r/declarativewidgets; $(PYTHON_SETUP_CMD) $(CMD)'
	@$(MAKE) clean-watch

run-test: SERVER_NAME?=urth_widgets_integration_test_server
run-test: sdist remove-server
	@echo $(TEST_MSG)
	@OPTIONS=-d SERVER_NAME=$(SERVER_NAME) $(MAKE) server$(JUPYTER)
	@echo 'Waiting for server to start...'
	@LIMIT=60; while [ $$LIMIT -gt 0 ] && ! docker logs $(SERVER_NAME) 2>&1 | grep 'Notebook is running'; do echo waiting $$LIMIT...; sleep 1; LIMIT=$$(expr $$LIMIT - 1); done
	@$(foreach browser, $(BROWSER_LIST), echo 'Running system integration tests on $(browser)...'; npm run system-test -- $(SPECS) --baseurl $(BASEURL) --test-type $(TEST_TYPE) --browser $(browser) || exit)
	@SERVER_NAME=$(SERVER_NAME) $(MAKE) remove-server

system-test-python3: TEST_MSG="Starting system tests for Python 3"
system-test-python3:
	TEST_MSG=$(TEST_MSG) TEST_TYPE=$(TEST_TYPE) BROWSER_LIST="$(BROWSER_LIST)" JUPYTER=$(JUPYTER) SPECS="$(SPECS)" BASEURL=$(BASEURL) $(MAKE) run-test

system-test-python2: PYTHON=python2
system-test-python2: SPECS:=$(PYTHON2_SPECS)
system-test-python2: TEST_MSG="Starting system tests for Python 2"
system-test-python2:
	@TEST_MSG=$(TEST_MSG) TEST_TYPE=$(TEST_TYPE) BROWSER_LIST="$(ALT_BROWSER_LIST)" JUPYTER=$(JUPYTER) SPECS="$(SPECS)" BASEURL=$(BASEURL) $(MAKE) run-test

system-test-alt-jupyter: JUPYTER:=_$(ALT_JUPYTER_VERSION)
system-test-alt-jupyter: SPECS:=$(ALT_JUPYTER_SPECS)
system-test-alt-jupyter: TEST_MSG="Starting system tests for Jupyter $(ALT_JUPYTER_VERSION)"
system-test-alt-jupyter:
	@TEST_MSG=$(TEST_MSG) TEST_TYPE=$(TEST_TYPE) BROWSER_LIST="$(ALT_BROWSER_LIST)" JUPYTER=$(JUPYTER) SPECS="$(SPECS)" BASEURL=$(BASEURL) $(MAKE) run-test

system-test-all: system-test-python3 system-test-python2 system-test-alt-jupyter

start-selenium: node_modules stop-selenium
	@echo "Installing and starting Selenium Server..."
	@node_modules/selenium-standalone/bin/selenium-standalone install >/dev/null
	@node_modules/selenium-standalone/bin/selenium-standalone start 2>/dev/null & echo $$! > SELENIUM_PID

stop-selenium:
	-@kill `cat SELENIUM_PID`
	-@rm SELENIUM_PID

system-test-all-local: TEST_TYPE:="local"
system-test-all-local: start-selenium system-test-all stop-selenium

system-test-all-remote: TEST_TYPE:="remote"
system-test-all-remote: system-test-all

system-test:
ifdef SAUCE_USER_NAME
	@echo 'Running system tests on Sauce Labs...'
	@BROWSER_LIST="$(BROWSER_LIST)" JUPYTER=$(JUPYTER) SPECS="$(SPECS)" BASEURL=$(BASEURL) $(MAKE) system-test-all-remote
else ifdef TRAVIS
	@echo 'Starting system integration tests locally on Travis...'
	@BROWSER_LIST="firefox" ALT_BROWSER_LIST="firefox" JUPYTER=$(JUPYTER) SPECS="$(SPECS)" BASEURL=$(BASEURL) $(MAKE) system-test-all-local
else
	@echo 'Starting system integration tests locally...'
	@BROWSER_LIST="$(BROWSER_LIST)" JUPYTER=$(JUPYTER) SPECS="$(SPECS)" BASEURL=$(BASEURL) $(MAKE) system-test-all-local
endif
	@echo 'System integration tests complete.'

docs: DOC_PORT?=4001
docs: DOCURL?=http://127.0.0.1
docs: .watch-docs dist/docs
	@echo "Serving docs at $(DOCURL):$(DOC_PORT)"
	@bash -c "trap 'make clean-watch-docs' INT TERM ; npm run http-server -- dist/docs/site -p $(DOC_PORT)"

all: init test-js-remote test-py-all test-scala test-r sdist install-all system-test

release: EXTRA_OPTIONS=-e PYPI_USER=$(PYPI_USER) -e PYPI_PASSWORD=$(PYPI_PASSWORD)
release: PRE_SDIST=echo "[server-login]" > ~/.pypirc; echo "username:" ${PYPI_USER} >> ~/.pypirc; echo "password:" ${PYPI_PASSWORD} >> ~/.pypirc;
release: POST_SDIST=register upload
release: sdist
