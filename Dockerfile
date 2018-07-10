# STAGE 1
FROM azul/zulu-openjdk-alpine:8

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

ENV GRADLE_USER_HOME "/usr/src/app/gradle_home"

# download everything before we copy source so we don't have to download each time we download source

# 1: download gradle
COPY gradlew ./
COPY gradle ./gradle
RUN ./gradlew

# 2: download build dependencies
COPY build.gradle settings.gradle ./
# Trick gradle to download all dependencies
COPY gradle-init ./src
RUN ./gradlew test shadowJar && rm -rfv src && rm -rfv build

# build source
COPY src ./src
RUN ./gradlew test shadowJar

# STAGE 2
FROM openjdk:8-jre-alpine

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

COPY --from=0 /usr/src/app/build/libs/configuration-server.jar ./