# Stage 1: Build
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY services/content-service/pom.xml services/content-service/pom.xml
COPY services/user-service/pom.xml services/user-service/pom.xml
COPY services/api-gateway/pom.xml services/api-gateway/pom.xml

RUN chmod +x mvnw && \
    ./mvnw dependency:go-offline -pl services/content-service -am

COPY services/content-service/src services/content-service/src

RUN ./mvnw clean package -DskipTests -pl services/content-service -am

# Stage 2: Runtime
FROM eclipse-temurin:17-jre

RUN apt-get update && apt-get install -y ffmpeg && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /app/services/content-service/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
