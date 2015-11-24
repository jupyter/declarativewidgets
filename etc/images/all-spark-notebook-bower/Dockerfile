# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.
FROM jupyter/all-spark-notebook:dbb9c7b50531

MAINTAINER Jupyter Incubator Projects

USER root

RUN apt-get install --yes npm && \
  apt-get install --yes nodejs && \
  ln -s /usr/bin/nodejs /usr/bin/node && \
  npm install -g bower && \
  apt-get clean
