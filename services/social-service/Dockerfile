# Stage 1: Build
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY services/social-service/pom.xml services/social-service/pom.xml
COPY services/user-service/pom.xml services/user-service/pom.xml
COPY services/api-gateway/pom.xml services/api-gateway/pom.xml
COPY services/content-service/pom.xml services/content-service/pom.xml

RUN chmod +x mvnw && \
    ./mvnw dependency:go-offline -pl services/social-service -am

COPY services/social-service/src services/social-service/src

RUN ./mvnw clean package -DskipTests -pl services/social-service -am

# Stage 2: Runtime
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/services/social-service/target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
