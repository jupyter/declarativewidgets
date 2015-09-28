# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.

FROM ubuntu:14.04

RUN apt-get update
RUN apt-get --no-install-recommends -y --force-yes install openjdk-7-jre libjansi-java curl git

ENV SCALA_VERSION 2.10.4
ENV SBT_VERSION 0.13.7

# Install Scala
RUN \
    cd /root && \
    curl -o scala-$SCALA_VERSION.tgz http://downloads.typesafe.com/scala/$SCALA_VERSION/scala-$SCALA_VERSION.tgz && \
    tar -xf scala-$SCALA_VERSION.tgz && \
    rm scala-$SCALA_VERSION.tgz && \
    echo >> /root/.bashrc && \
    echo 'export PATH=~/scala-$SCALA_VERSION/bin:$PATH' >> /root/.bashrc

# Install sbt
RUN \
    curl -L -o sbt-$SBT_VERSION.deb https://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb && \
    dpkg -i sbt-$SBT_VERSION.deb && \
    rm sbt-$SBT_VERSION.deb && \
    apt-get update && \
    apt-get install sbt

#Downloading kernel and publishing local
RUN \
    git clone https://github.com/ibm-et/spark-kernel.git && \
    cd spark-kernel && \
    sbt publish-local

CMD []