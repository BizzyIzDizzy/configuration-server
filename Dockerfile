# STAGE 1
FROM azul/zulu-openjdk-alpine:8

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

ENV GRADLE_USER_HOME "/usr/src/app/gradle_home"

# download gradle
COPY gradlew ./
COPY gradle ./gradle
RUN ./gradlew

COPY ./ ./
RUN ./gradlew test shadowJar

# STAGE 2
FROM openjdk:8-jre-alpine

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

COPY --from=0 /usr/src/app/web/build/libs/configuration-server.jar ./