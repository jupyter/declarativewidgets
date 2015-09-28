# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.

########## Spark Kernel Related Images ##########

.PHONY: clean pyspark-notebook-bower-sparkkernel sbt-sparkkernel-image

clean:
	@-rm -rf kernel/Makefile kernel/VERSION kernel/bin kernel/lib

kernel/bin/sparkkernel: REPO?=cloudet/sbt-sparkkernel-image
kernel/bin/sparkkernel:
	@echo 'Building spark kernel...'
	@docker run -it --rm \
		-v `pwd`/kernel:/home/jovyan/kernel \
		$(REPO) bash -c 'cd spark-kernel && \
			sbt compile && \
			sbt pack && \
			cd kernel/target/pack && \
			make install && \
			cp -R * /home/jovyan/kernel'

#Runtime Notebook Image with Spark Kernel
pyspark-notebook-bower-sparkkernel: kernel/bin/sparkkernel
	@docker build -t cloudet/pyspark-notebook-bower-sparkkernel .

#Dev Image for building Spark Kernel and Urth.Widgets scala support
sbt-sparkkernel-image:
	@echo 'Building scala/sbt image'
	@docker build -t cloudet/sbt-sparkkernel-image ./kernel