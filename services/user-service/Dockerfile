# Stage 1: Build
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Copy service source
COPY services/user-service/pom.xml services/user-service/pom.xml
COPY services/user-service/src services/user-service/src

RUN chmod +x mvnw && \
    ./mvnw clean package -DskipTests -pl services/user-service -am

# Stage 2: Runtime
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/services/user-service/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
