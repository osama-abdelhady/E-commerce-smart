# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Cache dependencies in their own layer: only re-downloads when pom.xml changes.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# curl for the HEALTHCHECK below — not present in the base JRE image by default.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Runs as a non-root user — never the container default root.
RUN groupadd -r ecommerce && useradd -r -g ecommerce ecommerce

COPY --from=build /workspace/target/*.jar app.jar
RUN chown ecommerce:ecommerce app.jar
USER ecommerce

EXPOSE 8080

# Matches management.endpoints.web.exposure.include: health,info from application.yml.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
