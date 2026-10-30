# Build both parts, then run them at one HTTPS origin behind Render's proxy.
FROM node:22-bookworm-slim AS frontend
WORKDIR /build/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-26 AS backend
WORKDIR /build/backend
COPY backend/pom.xml ./
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /build/frontend/dist ./src/main/resources/static
# PostgreSQL integration tests run in CI, not inside the image build.
RUN mvn --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd --gid 10001 ems && useradd --uid 10001 --gid ems --no-create-home ems
COPY --from=backend --chown=ems:ems /build/backend/target/employee-management-1.0.0.jar app.jar
USER ems
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
