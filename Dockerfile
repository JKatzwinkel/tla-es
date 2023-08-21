FROM gradle:8.3.0-jdk17 AS build

COPY --chown=gradle:gradle . /home/gradle/tla
WORKDIR /home/gradle/tla

RUN gradle bootJar --no-daemon && \
    mv build/libs/*.jar bin/run/tla-backend.jar


FROM openjdk:22-jdk-slim-bookworm

RUN mkdir /app
RUN apt-get update && apt-get install -y wget
COPY --from=build /home/gradle/tla/bin/run/ /app/
WORKDIR /app/

EXPOSE 8090
ENTRYPOINT ["sh", "/app/entrypoint.sh"]
