# Stage 1: Build stage
FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

# Copy root pom and module poms for layer caching
COPY pom.xml ./
COPY services/account-service/pom.xml services/account-service/
COPY services/payment-service/pom.xml services/payment-service/

# Download dependencies
RUN mvn dependency:go-offline -B -pl services/account-service -am

# Copy source code and build
COPY services/account-service/src services/account-service/src
RUN mvn clean package -DskipTests -pl services/account-service -am

# Stage 2: Minimal Distroless / Alpine Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root user
RUN addgroup -S nexapay && adduser -S nexapay -G nexapay
USER nexapay:nexapay

COPY --from=builder /workspace/services/account-service/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]