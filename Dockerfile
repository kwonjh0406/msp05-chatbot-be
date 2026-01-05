# Build Stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

# Grant execution rights on gradlew
RUN chmod +x ./gradlew
RUN ./gradlew bootJar -x test

# Run Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

# We expect application.yaml to be mounted or copied if needed, 
# but usually it's embedded or overridden by secrets. 
# Here we just run the jar.

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
