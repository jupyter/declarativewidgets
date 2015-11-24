# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.

.PHONY: build dev help build push tag

OWNER:=cloudet
IMAGE:=all-spark-notebook-bower
VERSION:=1.5.1_jupyter4 # spark version
COMMIT:=$(shell git rev-parse --short=12 --verify HEAD)

help:
	@echo
	@echo '   build   - builds the latest image for the stack'
	@echo '     dev   - runs a foreground container for the stack'
	@echo '    push   - pushes the latest and HEAD git SHA tags for the stack to Docker Hub'
	@echo '    tag    - tags the latest stack image with the HEAD git SHA'

build: DARGS?=
build:
	docker build $(DARGS) --rm --force-rm -t $(OWNER)/$(IMAGE):latest .

dev: ARGS?=
dev: DARGS?=
dev:
	docker run -it --rm -p 8888:8888 $(DARGS) $(OWNER)/$(IMAGE) $(ARGS)

push:
	docker push $(OWNER)/$(IMAGE):latest
	docker push $(OWNER)/$(IMAGE):$(VERSION)
	docker push $(OWNER)/$(IMAGE):$(COMMIT)

tag:
	docker tag $(OWNER)/$(IMAGE):latest $(OWNER)/$(IMAGE):$(VERSION)
	docker tag $(OWNER)/$(IMAGE):latest $(OWNER)/$(IMAGE):$(COMMIT)
