# Build stage
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /build/target/questionary-1.0.jar app.jar

# Mount point for the SQLite database file
VOLUME /db

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
