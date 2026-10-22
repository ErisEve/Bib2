# syntax=docker/dockerfile:1.7
# ---- build stage -------------------------------------------------------------
# Built with the maven image rather than ./mvnw: the wrappers are non-executable,
# ship as only-script (no maven-wrapper.jar), disagree on version across modules,
# and BibliotekaUI has no wrapper at all.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY src ./src
# -DskipTests is required, not an optimisation: the only test in every module is a
# @SpringBootTest contextLoads() that needs PostgreSQL and Eureka reachable.
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package

# ---- runtime stage -----------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app

# Glob, not an explicit name: four of the seven modules have an artifactId that
# does not match their directory name.
COPY --from=build /build/target/*.jar app.jar

USER app
EXPOSE 8081

# JAVA_TOOL_OPTIONS instead of a shell-form entrypoint, so java stays PID 1 and
# SIGTERM from Docker/Kubernetes reaches the JVM.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

HEALTHCHECK --interval=15s --timeout=3s --start-period=90s --retries=10 \
  CMD wget -qO- http://127.0.0.1:8081/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java","-jar","/app/app.jar"]
