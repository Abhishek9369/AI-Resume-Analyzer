# Multi-stage Dockerfile for Resume Analyzer
# Stage 1: Build the application with Maven (includes frontend build via frontend-maven-plugin)

FROM maven:3.9-eclipse-temurin-21 AS builder

LABEL stage=builder
LABEL description="Build stage for Resume Analyzer application"

# Set working directory
WORKDIR /app

# Copy Maven project files
COPY pom.xml .
COPY src ./src

# Build the application (this will also build the frontend)
# Skip tests during Docker build for faster builds
RUN mvn clean package -DskipTests

# Verify the JAR was created
RUN ls -lh /app/target/*.jar

# Stage 2: Runtime image with JRE only
FROM eclipse-temurin:21-jre-jammy

LABEL maintainer="Resume Analyzer Team"
LABEL description="Resume Analyzer - AI-powered resume analysis application"
LABEL version="1.0.0"

# Create a non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Create directories for file uploads and logs
RUN mkdir -p /app/uploads /app/logs && \
    chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM options for containerized environment
ENV JAVA_OPTS="-Xmx1g -Xms512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
