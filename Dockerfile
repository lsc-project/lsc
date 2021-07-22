FROM openjdk:8-jdk-alpine
RUN apk update
RUN apk upgrade
RUN apk add bash
RUN apk add curl

ADD lsc-2.1.5 /opt/lsc-2.1.5
RUN chmod a+x /opt/lsc-2.1.5/*
RUN chmod +x /opt/lsc-2.1.5/entrypoint.sh
WORKDIR /opt/lsc-2.1.5/bin/

ENV JAVA_OPTS="-DLSC.PLUGINS.PACKAGEPATH=org.lsc.plugins.connectors.james.generated" 
ENV CONF_DIR=/opt/lsc-2.1.5/conf/ldap-to-james-user
#RUN ./lsc --config /opt/lsc-2.1.5/sample/ldap-to-james-user/ --synchronize all --clean all --threads 1

ENTRYPOINT ["/opt/lsc-2.1.5/entrypoint.sh"]

EXPOSE 389

