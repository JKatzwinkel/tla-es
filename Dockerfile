FROM gradle:7.4.2-jdk17 AS build

COPY --chown=gradle:gradle . /home/gradle/tla
WORKDIR /home/gradle/tla

RUN gradle bootJar --no-daemon && \
    mv build/libs/*.jar bin/run/tla-backend.jar


FROM openjdk:17.0.2-jdk-buster

RUN mkdir /app
WORKDIR /app/
COPY --from=build /home/gradle/tla/bin/run/ /app/

EXPOSE 8090
ENTRYPOINT ["sh", "/app/entrypoint.sh"]
