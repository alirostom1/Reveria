# Stage 1: Build
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY services/api-gateway/pom.xml services/api-gateway/pom.xml

RUN chmod +x mvnw && \
    ./mvnw dependency:go-offline -pl services/api-gateway -am

COPY services/api-gateway/src services/api-gateway/src

RUN ./mvnw clean package -DskipTests -pl services/api-gateway -am

# Stage 2: Runtime
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/services/api-gateway/target/*.jar app.jar

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "app.jar"]

