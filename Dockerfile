FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml .
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src ./src
# Tests use an isolated in-memory database; PostgreSQL is checked at runtime.
RUN mvn --batch-mode --no-transfer-progress clean verify

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S hms && adduser -S hms -G hms
COPY --from=builder --chown=hms:hms /app/target/hospital-management-system.jar app.jar

USER hms
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
