# ===================================================================
# Multi-stage Dockerfile for LendLoop (Spring Boot + Java 21)
# ===================================================================

# -------------------------------------------------------------------
# Stage 1: Build the application using Maven Wrapper
# -------------------------------------------------------------------
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copy Maven wrapper configuration and POM file
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Ensure Linux line endings and executable permissions for Maven wrapper
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Copy application source code
COPY src/ ./src/

# Build and package the Spring Boot executable JAR (skip unit tests during image build)
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

# Launch Spring Boot with dynamic PORT resolution
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8081} -jar app.jar"]
