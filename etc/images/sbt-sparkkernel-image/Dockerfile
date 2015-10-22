# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.

FROM debian:jessie

USER root

# Install basic packages
ENV DEBIAN_FRONTEND noninteractive
RUN apt-get update && apt-get install -yq --no-install-recommends \
    git \
    curl \
    build-essential \
    && apt-get clean

# Install JDK
RUN apt-get -y update && \
    apt-get install -y --no-install-recommends openjdk-7-jre-headless && \
    apt-get clean

# Install Scala
ENV SCALA_VERSION 2.10.4
RUN \
    cd /root && \
    curl -o scala-$SCALA_VERSION.tgz http://downloads.typesafe.com/scala/$SCALA_VERSION/scala-$SCALA_VERSION.tgz && \
    tar -xf scala-$SCALA_VERSION.tgz && \
    rm scala-$SCALA_VERSION.tgz && \
    echo >> /root/.bashrc && \
    echo 'export PATH=~/scala-$SCALA_VERSION/bin:$PATH' >> /root/.bashrc

# Install sbt
ENV SBT_VERSION 0.13.7
RUN \
    curl -L -o sbt-$SBT_VERSION.deb https://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb && \
    dpkg -i sbt-$SBT_VERSION.deb && \
    rm sbt-$SBT_VERSION.deb && \
    apt-get update && \
    apt-get install sbt

#Downloading kernel and publishing local
ENV APACHE_SPARK_VERSION 1.5.1
RUN \
    git clone https://github.com/ibm-et/spark-kernel.git && \
    cd spark-kernel && \
    sbt publish-local

CMD []