FROM docker.dbc.dk/payara-micro as base

USER root

RUN apt-install libxml2 libxslt1.1

#https://issues.jenkins-ci.org/browse/JENKINS-44609
FROM base as yaz4jbuilder

RUN apt-install autoconf automake libtool gcc bison tclsh xsltproc docbook docbook-xml docbook-xsl libxslt1-dev libreadline-dev libwrap0-dev pkg-config libicu-dev make git swig g++ ca-certificates ca-certificates-java

WORKDIR /tmp

RUN git clone https://github.com/DBCDK/yaz.git
RUN cd yaz ; git checkout -b compile origin/sb-toKeep-quick-fix && ./buildconf.sh && ./configure --prefix=/usr/local && make install

COPY files/yaz4j-1.5.tar.gz files/apache-maven-3.5.0-bin.tar.gz /tmp/
RUN for f in *.tar.gz ; do tar xzf $f ; done
RUN cp /usr/local/lib/*.so* /usr/lib/
RUN cd yaz4j-1.5 ; ../apache*/bin/mvn package

#https://issues.jenkins-ci.org/browse/JENKINS-44609
FROM base

COPY --from=yaz4jbuilder /usr/local/lib/*.so* /tmp/yaz4j-1.5/./unix/target/libyaz4j.so /usr/lib/
COPY --from=yaz4jbuilder /tmp/yaz4j-1.5/any/target/yaz4j.jar /payara-micro/jars/


WORKDIR /payara-micro
USER gfish
ADD z3950-ill-proxy-1.0-SNAPSHOT.war wars/
CMD ./start-payara-micro.sh
