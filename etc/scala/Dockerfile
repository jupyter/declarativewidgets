# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.

FROM cloudet/pyspark-notebook-bower

ENV USR_HOME /home/jovyan

COPY kernel ${USR_HOME}/kernel
COPY kernel.json ${USR_HOME}/.ipython/kernels/spark/kernel.json