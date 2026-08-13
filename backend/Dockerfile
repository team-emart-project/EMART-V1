# syntax=docker/dockerfile:1

# ---------------------------------------------------------------- build stage
# Maven and the JDK are only needed to produce the jar. They add ~700MB that
# has no business being in the image that actually runs in production, hence
# the second stage below.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
COPY src ./src

# The BuildKit cache mount keeps ~/.m2 on the builder between builds, so a
# rebuild after a source change re-downloads nothing. It is deliberately NOT a
# COPY-pom-then-go-offline layer: go-offline is fragile with Spring Boot 4's
# newer starters, and the cache mount gives the same speed-up without it.
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package

# ------------------------------------------------------------------ run stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# curl is for the compose healthcheck only. The jre image does not ship it.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

# Never run the app as root. A compromised process should not own the filesystem.
RUN useradd --system --create-home --shell /usr/sbin/nologin emart
USER emart

# Only one .jar is produced; spring-boot-maven-plugin's *.jar.original is not
# matched by this glob.
COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080

# Container memory is the real limit here, not the JVM's default 1/4-of-host
# heuristic, so let the JVM read the cgroup and take 75% of it.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
