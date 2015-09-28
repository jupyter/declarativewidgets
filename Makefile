# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.

.PHONY: help clean sdist dist dev docs test test-js test-py test-scala init server install dev-build
.SUFFIXES:
MAKEFLAGS=-r

help:
	@echo '      init - setups machine with base requirements for dev'
	@echo '     clean - clean build files'
	@echo '       dev - start container with source mounted for development'
	@echo '      docs - start container that serves documentation'
	@echo '     sdist - build a source distribution'
	@echo '   install - install latest sdist into a container'
	@echo '    server - starts a container with extension installed through pip'
	@echo '      test - run unit tests'

init: node_modules

node_modules: package.json
	@npm install

node_modules/bower: node_modules

URTH_BOWER_FILES:=$(shell find elements -name bower.json)
URTH_SRC_DIRS:=$(foreach dir, $(URTH_BOWER_FILES), $(shell dirname $(dir)))
URTH_DIRS:=$(foreach dir, $(URTH_SRC_DIRS), $(shell basename $(dir)))
URTH_COMP_LINKS:=$(foreach dir, $(URTH_DIRS), $(shell echo "bower_components/$(dir)"))
NPM_BIN_DIR:=$(shell npm bin)
$(URTH_COMP_LINKS): | node_modules/bower $(URTH_SRC_DIRS)
	@echo 'Linking local Urth elements'
	@$(foreach dir, $(URTH_SRC_DIRS), cd $(abspath $(dir)) && $(NPM_BIN_DIR)/bower link;)
	@$(foreach name, $(URTH_DIRS), $(NPM_BIN_DIR)/bower link $(name);)

bower_components: node_modules/bower bower.json
	@npm run bower -- install

clean:
	@-rm -rf dist
	@-rm -rf *.egg-info
	@-rm -rf __pycache__ */__pycache__ */*/__pycache__
	@-find . -name '*.pyc' -exec rm -fv {} \;
	@-rm -rf bower_components node_modules

.watch: node_modules
	@echo 'Doing watch'
	@npm run watch &
	@touch .watch

.killwatch:
	@echo 'Killing watch'
	-@rm .watch
	-@kill -9 `pgrep gulp`

dev: REPO?=cloudet/pyspark-notebook-bower-sparkkernel
dev: NB_HOME?=/home/jovyan/.ipython
dev: NB_PORT=8888
dev: DEV_NAME?=urth_widgets_dev
dev: CMD?=ipython notebook --no-browser --port 8888 --ip="*"
dev: .watch dist
	@docker run -it --rm --name $(DEV_NAME) \
		-p $(NB_PORT):8888 \
		-e USE_HTTP=1 \
		-e JVM_OPT=-Dlog4j.logLevel=trace \
		-v `pwd`/dist/urth_widgets:$(NB_HOME)/nbextensions/urth_widgets \
		-v `pwd`/dist/urth:/opt/conda/lib/python3.4/site-packages/urth \
		-v `pwd`/dist/urth-widgets.jar:/home/jovyan/kernel/lib/urth-widgets.jar \
		-v `pwd`/etc:$(NB_HOME)/profile_default/nbconfig \
		-v `pwd`/etc/ipython_notebook_config.py:$(NB_HOME)/profile_default/ipython_notebook_config.py \
		-v `pwd`/notebooks:/home/jovyan/work \
		$(REPO) bash -c 'git config --global core.askpass true && \
			$(CMD)'
	@make .killwatch

dist/urth_widgets/js: ${shell find nb-extension}
	@echo 'Moving src/nb-extension'
	@mkdir -p dist/urth_widgets/js
	@cp -R nb-extension/js/* dist/urth_widgets/js/.

dist/urth_widgets/elements: ${shell find elements}
	@echo 'Moving elements'
	@mkdir -p dist/urth_widgets/elements
	@cp -R elements/* dist/urth_widgets/elements/.
	@touch dist/urth_widgets/elements

dist/urth_widgets/bower_components: $(URTH_COMP_LINKS)
	@echo 'Moving bower_components'
	@mkdir -p dist/urth_widgets/bower_components
	@cp -RL bower_components/* dist/urth_widgets/bower_components/.

dist/urth_widgets: dist/urth_widgets/bower_components dist/urth_widgets/js dist/urth_widgets/elements

dist/urth/widgets/urth_import.py: nb-extension/python/urth/widgets/urth_import.py
	@echo 'Moving frontend extension code'
	@mkdir -p dist/urth/widgets
	@cp -R nb-extension/python/urth/widgets/urth_import.py dist/urth/widgets/urth_import.py

dist/urth: ${shell find kernel-python/urth} dist/urth/widgets/urth_import.py
	@echo 'Moving python code'
	@mkdir -p dist/urth
	@cp -R kernel-python/urth/* dist/urth/.

dist/urth_widgets/urth-widgets.jar: REPO?=cloudet/sbt-sparkkernel-image
dist/urth_widgets/urth-widgets.jar: ${shell find kernel-scala/src/main/scala/}
	@echo 'Building scala code'
	@mkdir -p dist
	@docker run -it --rm \
		-v `pwd`:/src \
		$(REPO) bash -c 'cp -r /src/kernel-scala /tmp/src && \
			cd /tmp/src && \
			sbt package && \
			cp target/scala-2.10/urth-widgets*.jar /src/dist/urth_widgets/urth-widgets.jar'

dist/docs: bower_components ${shell find elements/**/*.html} bower.json etc/docs/index.html etc/docs/urth-docs.html
	@echo 'Building docs'
	@mkdir -p dist/docs
	@cp elements/**/*.html dist/docs
	@cp -R etc/docs/*.html dist/docs
	@cp bower.json dist/docs
	@cp -RLf bower_components dist/docs

dist: dist/urth_widgets dist/urth dist/urth_widgets/urth-widgets.jar dist/docs

sdist: REPO?=jupyter/pyspark-notebook:3.2
sdist: RELEASE?=
sdist: GIT_COMMIT?=HEAD
sdist: BUILD_NUMBER?=0
sdist: dist
	@cp -R MANIFEST.in dist/.
	@cp -R setup.py dist/.
	@docker run -it --rm \
		-v `pwd`/dist:/src \
		$(REPO) bash -c 'cp -r /src /tmp/src && \
			cd /tmp/src && \
			echo "$(BUILD_NUMBER)-$(GIT_COMMIT)" > VERSION && \
			python setup.py sdist && \
			cp -r dist/*.tar.gz /src/.'

test: REPO?=jupyter/pyspark-notebook:3.2
test: test-js test-py test-scala

test-js: | $(URTH_COMP_LINKS)
	@echo 'Running web component tests...'
	@npm run test

test-py: REPO?=jupyter/pyspark-notebook:3.2
test-py: dist/urth
	@echo 'Running python tests...'
	@docker run -it --rm \
			-v `pwd`/dist/urth:/usr/local/lib/python3.4/dist-packages/urth \
			$(REPO) bash -c 'python3 -m unittest discover /usr/local/lib/python3.4/dist-packages/urth'

test-scala: REPO?=cloudet/sbt-sparkkernel-image
test-scala:
	@echo 'Running scala tests...'
	@docker run -it --rm \
		-v `pwd`/kernel-scala:/src \
		$(REPO) bash -c 'cp -r /src /tmp/src && \
			cd /tmp/src && \
			sbt test'

testdev: | $(URTH_COMP_LINKS)
	@npm run test -- -p

install: REPO?=cloudet/pyspark-notebook-bower-sparkkernel
install: CMD?=exit
install:
	@docker run -it --rm \
		-v `pwd`:/src \
		$(REPO) bash -c 'cd /src/dist && \
			pip install $$(ls -1 *.tar.gz | tail -n 1) && \
			$(CMD)'

server: REPO?=cloudet/pyspark-notebook-bower-sparkkernel
server: CMD?=ipython notebook --no-browser --port 8888 --ip="*"
server: SERVER_NAME?=urth_widgets_server
server:
	@echo 'Starting server...'
	@docker run -it --rm --name $(SERVER_NAME) \
		-p 9500:8888 \
		-e USE_HTTP=1 \
		-v `pwd`:/widgets-nbexts \
		-v `pwd`/notebooks:/home/jovyan/work \
		$(REPO) bash -c 'git config --global core.askpass true && \
			pip install $$(ls -1 /widgets-nbexts/dist/*.tar.gz | tail -n 1) && \
			$(CMD)'

docs: DOC_PORT?=4001
docs: DOC_NAME?=urth_widgets_docs
docs: DOC_IMAGE?=urth_polyserve
docs: DOC_DIR?=/src/urth-widgets
docs: DOC_PARAMS?=-it --rm
docs: .watch dist/docs
	-@docker rm -f $(DOC_NAME)
	@docker build -t $(DOC_IMAGE) etc/docs
	-@docker run $(DOC_PARAMS) --name $(DOC_NAME) \
		-p $(DOC_PORT):8080 \
		-v `pwd`/dist/docs:$(DOC_DIR):ro \
		$(DOC_IMAGE) bash -c 'cd $(DOC_DIR) && \
			echo "Documentation available at <ip_address>:$(DOC_PORT)/components/urth-widgets/" && \
			polyserve >> /dev/null'
	@make .killwatch
