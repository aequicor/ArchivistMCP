# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM gradle:9.4.1-jdk17 AS builder

WORKDIR /app

COPY build.gradle.kts settings.gradle.kts ./
COPY gradle/ gradle/
RUN gradle dependencies --no-daemon -Dorg.gradle.java.installations.auto-download=false || true

COPY src/ src/
RUN gradle shadowJar --no-daemon -Dorg.gradle.java.installations.auto-download=false

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/build/libs/*-all.jar app.jar
RUN java -cp app.jar dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel || true
ENTRYPOINT ["java", "-jar", "app.jar", "--stdio", "8080"]