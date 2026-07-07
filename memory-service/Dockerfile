FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/target/memory-service-0.1.0-SNAPSHOT.jar /app/memory-service.jar
USER app

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/memory-service.jar"]

