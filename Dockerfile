FROM ubuntu:16.04

MAINTAINER Chernov Andrey

RUN apt-get -y update

ENV PGVER 9.5
RUN apt-get install -y postgresql-$PGVER

USER postgres

RUN /etc/init.d/postgresql start &&\
	psql --command "CREATE USER docker with SUPERUSER PASSWORD 'docker';" &&\
	createdb -E UTF8 -T template0 -O docker docker &&\
	/etc/init.d/postgresql stop


RUN echo "host all all 0.0.0.0/0 md5" >> etc/postgresql/$PGVER/main/pg_hba.conf
RUN sed -i 's/peer/md5/g' /etc/postgresql/$PGVER/main/pg_hba.conf
RUN echo "listen_addresses='*'" >> /etc/postgresql/$PGVER/main/postgresql.conf

EXPOSE 5432

VOLUME  ["/etc/postgresql", "/var/log/postgresql", "/var/lib/postgresql"]

USER root

RUN apt-get install -y openjdk-8-jdk-headless
RUN apt-get install -y maven

ENV WORK /opt/springPostgreSQL
ADD pom.xml $WORK/spring-postgresql/pom.xml
ADD src/    $WORK/spring-postgresql/src/


WORKDIR $WORK/spring-postgresql
RUN mvn package

EXPOSE 5000

CMD service postgresql start && java -Xmx300M -Xms300M -jar $WORK/spring-postgresql/target/spring-postgresql-1.0-SNAPSHOT.jar --database=jdbc:postgresql://localhost/docker --username=docker --password=docker