# filepath: /backend/Dockerfile
# Build: docker build -t sakutahu-backend ./backend   (biasanya lewat docker-compose.prod.yml)

# ---------- 1. Build jar ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Layer dependency di-cache selama pom.xml tidak berubah
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
# Test dijalankan di CI (GitHub Actions), bukan saat build image
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package \
    && cp target/sakutahu-backend-*.jar /app/app.jar

# ---------- 2. Runtime ----------
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /app/app.jar app.jar
USER app

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD wget -qO- http://127.0.0.1:8080/actuator/health/liveness >/dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
