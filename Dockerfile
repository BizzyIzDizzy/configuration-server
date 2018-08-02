# STAGE 1
FROM azul/zulu-openjdk-alpine:8

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

ENV GRADLE_USER_HOME "/usr/src/app/gradle_home"

# download gradle
COPY gradlew ./
COPY gradle ./gradle
RUN ./gradlew

# download dependencies
COPY settings.gradle build.gradle ./
COPY ./api/build.gradle ./api/
COPY ./core/build.gradle ./core/
COPY ./utils/build.gradle ./utils/
COPY ./web/build.gradle ./web/
RUN ./gradlew resolveDependencies

# now copy source and build
COPY ./ ./
RUN ./gradlew test shadowJar

# STAGE 2
FROM openjdk:8-jre-alpine

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

COPY --from=0 /usr/src/app/web/build/libs/configuration-server.jar ./