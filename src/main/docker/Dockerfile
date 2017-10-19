FROM docker.dbc.dk/payara-micro as base

USER root

COPY files/base_apt_setup /

RUN apt-install libyaz5

FROM base as yaz4jbuilder

RUN apt-install libyaz5-dev swig g++ 

WORKDIR /tmp
COPY files/yaz4j-1.5.tar.gz files/apache-maven-3.5.0-bin.tar.gz /tmp/
RUN for f in *.tar.gz ; do tar xzf $f ; done
RUN cd yaz4j-1.5 ; ../apache*/bin/mvn package

FROM base

COPY --from=yaz4jbuilder /tmp/yaz4j-1.5/any/target/yaz4j.jar /payara-micro/jars
COPY --from=yaz4jbuilder /tmp/yaz4j-1.5/./unix/target/libyaz4j.so /usr/lib

WORKDIR /payara-micro
USER gfish
ADD z3950-ill-proxy-1.0-SNAPSHOT.war wars/
CMD ./start-payara-micro.sh
