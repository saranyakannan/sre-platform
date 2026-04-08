FROM eclipse-temurin:17-jdk-alpine as builder
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

RUN chmod +x gradlew
RUN ./gradlew bootJar -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Node.js for Cloud Observability MCP
RUN apk add --no-cache nodejs npm

# Pre-install MCP server
RUN npx -y @google-cloud/observability-mcp \
    --version || true

COPY --from=builder \
    /app/build/libs/sre-agent-1.0-SNAPSHOT.jar \
    app.jar

EXPOSE 8080

CMD java -Dserver.port=${PORT:-8080} -jar app.jar