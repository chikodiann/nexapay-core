# Stage 1: Build stage
FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

# Copy root pom and module poms for layer caching
COPY pom.xml ./
COPY services/account-service/pom.xml services/account-service/
COPY services/payment-service/pom.xml services/payment-service/

# Download dependencies
RUN mvn dependency:go-offline -B -pl services/payment-service -am

# Copy source code and build
COPY services/payment-service/src services/payment-service/src
RUN mvn clean package -DskipTests -pl services/payment-service -am

# Stage 2: Minimal Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root user
RUN addgroup -S nexapay && adduser -S nexapay -G nexapay
USER nexapay:nexapay

COPY --from=builder /workspace/services/payment-service/target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]