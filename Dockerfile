# ===================================================================
# Multi-stage Dockerfile for LendLoop (Spring Boot + Java 21)
# Optimized for Render Web Service Deployment & Local Docker
# ===================================================================

# -------------------------------------------------------------------
# Stage 1: Build the application using Maven Wrapper
# -------------------------------------------------------------------
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copy Maven wrapper configuration and POM file
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Normalize line endings (CRLF to LF) and grant execution permissions
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Copy application source code
COPY src/ ./src/

# Package the Spring Boot executable JAR (skipping tests for build speed)
RUN ./mvnw clean package -DskipTests

# -------------------------------------------------------------------
# Stage 2: Create lightweight Java 21 runtime container
# -------------------------------------------------------------------
FROM eclipse-temurin:21-jre

WORKDIR /app

# Create a dedicated non-root user and group for security
RUN groupadd -r lendloop && useradd -r -g lendloop lendloop

# Copy the compiled JAR artifact from the builder stage
COPY --from=builder --chown=lendloop:lendloop /app/target/*.jar app.jar

# Switch to non-root user
USER lendloop

# Default port configuration with 8081 fallback
ENV PORT=8081
EXPOSE 8081

# Launch Spring Boot with dynamic PORT binding and container-aware memory tuning
ENTRYPOINT ["sh", "-c", "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Dserver.port=${PORT:-8081} -jar app.jar"]
