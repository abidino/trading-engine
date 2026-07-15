# syntax=docker/dockerfile:1

# ═══════════════════════════════════════════════════════════════════════════════
# GraalVM native-image build for the Trading Engine.
#
#   docker build -f Dockerfile -t trading-engine:native .
#
# The build stage needs plenty of RAM (native-image typically wants 8–16 GB).
# The final image is a tiny, JIT-free binary that starts in ~0.1s and idles on a
# fraction of the JVM's memory. Secrets are supplied at runtime via env vars
# (see application-prod.yml) — nothing is baked into the image.
# ═══════════════════════════════════════════════════════════════════════════════

# ---- Build stage: compile the native binary ----
FROM ghcr.io/graalvm/graalvm-community:21 AS build
WORKDIR /workspace

# Warm the dependency cache separately from sources for faster rebuilds.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline

# Compile + Spring AOT + native-image. -Pnative activates the parent's AOT wiring.
COPY src/ src/
RUN ./mvnw -Pnative -DskipTests native:compile

# ---- Runtime stage: minimal image with only the binary + native deps ----
FROM debian:bookworm-slim
RUN apt-get update \
    && apt-get install -y --no-install-recommends libstdc++6 zlib1g ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Run as a non-root user.
RUN useradd --system --uid 1001 --create-home appuser
WORKDIR /app
COPY --from=build /workspace/target/trading-engine ./trading-engine
RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["/app/trading-engine"]
