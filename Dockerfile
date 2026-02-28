FROM gradle:8.5-jdk21-alpine AS build
WORKDIR /app
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle .
COPY settings.gradle .
COPY src ./src
RUN chmod +x gradlew
RUN ./gradlew bootJar
FROM eclipse-temurin:21-jre-alpine
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8001
ENTRYPOINT ["java","-jar","/app.jar"]
