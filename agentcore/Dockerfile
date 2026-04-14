FROM --platform=linux/arm64 eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .. .
RUN ./gradlew :server:bootJar --no-daemon

FROM --platform=linux/arm64 amazoncorretto:25-al2023-headless
WORKDIR /app
COPY --from=build /app/server/build/libs/*.jar app.jar
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "app.jar"]
