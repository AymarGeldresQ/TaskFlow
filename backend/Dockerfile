# ── Build stage ───────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY checkstyle ./checkstyle

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon -q

COPY src ./src

RUN ./gradlew bootJar --no-daemon -x test -x checkstyleMain -x spotbugsMain

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S taskflow && adduser -S taskflow -G taskflow

WORKDIR /app

COPY --from=builder --chown=taskflow:taskflow /app/build/libs/*.jar app.jar

USER taskflow

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
