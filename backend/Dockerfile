FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S cinema && adduser -S cinema -G cinema
COPY --from=build --chown=cinema:cinema /app/target/*.jar app.jar
USER cinema
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
