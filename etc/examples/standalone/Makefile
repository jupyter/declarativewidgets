# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.

.PHONY: help clean build run

clean: clean-npm clean-build

clean-npm:
	-@rm -r node_modules

clean-build:
	-@rm -r es5
	-@rm index.built.js

build-ipywidgets:
	@echo 'Building ipywidgets'
	cd ../../../../ipywidgets/ipywidgets && npm install

build-urth-widgets:
	@echo 'Building urth-widgets'
	cd ../../.. && make dist NOSCALA=true && npm install

build: build-urth-widgets build-ipywidgets
	@echo 'Building example'
	npm install
	-cd node_modules/urth-widgets/dist/urth_widgets && ln -s bower_components/ urth_components

run:
	python -m SimpleHTTPServer 8000
