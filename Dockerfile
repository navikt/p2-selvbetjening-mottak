FROM navikt/java:8

ARG version
ARG app_name

ENV LC_ALL="no_NB.UTF-8"
ENV LANG="no_NB.UTF-8"
ENV TZ="Europe/Oslo"
COPY service/target/$app_name-$version.jar "/app/application.jar"
CMD java -jar $DEFAULT_JAVA_OPTS $JAVA_OPTS /app/application.jar