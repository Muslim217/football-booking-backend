# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Кешируем зависимости (pom.xml меняется реже, чем код)
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Копируем исходники и собираем JAR
COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: Runtime ───────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Создаём непривилегированного пользователя
RUN addgroup -S app && adduser -S app -G app

# Директория для загружаемых файлов
RUN mkdir -p /app/uploads && chown app:app /app/uploads

COPY --from=builder /app/target/*.jar app.jar
RUN chown app:app app.jar

USER app

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
