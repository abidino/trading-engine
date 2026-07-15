# syntax=docker/dockerfile:1

# ═══════════════════════════════════════════════════════════════════════════════
# JVM build for the Trading Engine (Spring Boot executable jar).
#
#   docker build -t trading-engine .
#
# A plain JRE image — reliable on memory-constrained PaaS build machines
# (Render / Railway / Fly) unlike GraalVM native-image, which needs 8-16 GB RAM.
# Secrets are supplied at runtime via env vars (see application-prod.yml);
# nothing is baked into the image.
# ═══════════════════════════════════════════════════════════════════════════════

# ---- Build stage: compile + repackage the executable jar ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Warm the dependency cache separately from sources for faster rebuilds.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

# Compile + repackage. Tests are run in CI, skipped here for faster image builds.
COPY src/ src/
RUN ./mvnw -q -B -DskipTests clean package

# ---- Runtime stage: minimal JRE image with just the jar ----
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 --create-home appuser
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
