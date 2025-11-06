FROM eclipse-temurin:25-jdk AS build

COPY . /home/gradle/tla
WORKDIR /home/gradle/tla

RUN ./gradlew bootJar --no-daemon && \
    mv build/libs/*.jar bin/run/tla-backend.jar


FROM eclipse-temurin:25-jre

RUN mkdir /app
RUN apt-get update && apt-get install -y --no-install-recommends wget=1.21.4-1ubuntu4.1
COPY --from=build /home/gradle/tla/bin/run/ /app/
WORKDIR /app/

EXPOSE 8090
ENTRYPOINT ["sh", "/app/entrypoint.sh"]
