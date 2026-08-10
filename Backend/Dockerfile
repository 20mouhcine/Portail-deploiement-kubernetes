# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S kubeportal && adduser -S -G kubeportal -u 10001 kubeportal
COPY --from=build /workspace/target/Backend-*.jar /app/backend.jar

USER 10001:10001
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/backend.jar"]
