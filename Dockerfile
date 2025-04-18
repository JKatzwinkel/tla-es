FROM openjdk:23-jdk-slim-bookworm AS build

COPY . /home/gradle/tla
WORKDIR /home/gradle/tla

RUN ./gradlew bootJar --no-daemon && \
    mv build/libs/*.jar bin/run/tla-backend.jar


FROM openjdk:23-jdk-slim-bookworm

RUN mkdir /app
RUN apt-get update && apt-get install -y --no-install-recommends wget=1.21.3-1+deb12u1
COPY --from=build /home/gradle/tla/bin/run/ /app/
WORKDIR /app/

EXPOSE 8090
ENTRYPOINT ["sh", "/app/entrypoint.sh"]
