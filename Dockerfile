FROM gradle:8.12.1-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle shadowJar -x test --no-daemon

FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=build /app/build/libs/app.jar /app/app.jar

COPY .env /app/.env
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]