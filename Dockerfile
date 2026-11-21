# syntax=docker/dockerfile:1

# --- Build stage -----------------------------------------------------------
# Runs on the builder's own platform: the jar is architecture-independent, so a
# cross-build (e.g. linux/arm64 on an amd64 CI runner) doesn't emulate Maven.
FROM --platform=$BUILDPLATFORM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -q clean package -DskipTests

# --- Runtime stage -----------------------------------------------------------
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S gravita && adduser -S gravita -G gravita
COPY --from=build /app/target/*.jar app.jar
USER gravita

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
