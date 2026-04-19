FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl

COPY build/libs/ArchivistMCP-0.1.0-all.jar app.jar

ENV JAVA_OPTS="-Xmx512m"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --sse-server-ktor 8080"]