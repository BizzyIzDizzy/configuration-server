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
COPY ./server/build.gradle ./server/
COPY ./plugins/build.gradle ./plugins/
COPY ./plugins/file-system-loader/build.gradle ./plugins/file-system-loader/
COPY ./plugins/javascript-expression-formatter/build.gradle ./plugins/javascript-expression-formatter/
COPY ./plugins/properties-parser/build.gradle ./plugins/properties-parser/
COPY ./plugins/json-parser/build.gradle ./plugins/json-parser/
RUN ./gradlew resolveDependencies

# now copy source and build
COPY ./ ./
RUN ./gradlew test build

# STAGE 2
FROM openjdk:8-jre-alpine

RUN mkdir -p /usr/src/app/plugins/bin
WORKDIR /usr/src/app

COPY --from=0 /usr/src/app/plugins/bin/ ./plugins/bin/
COPY --from=0 /usr/src/app/server/build/libs/configuration-server.jar ./

EXPOSE 8080