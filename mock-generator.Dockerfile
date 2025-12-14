FROM eclipse-temurin:17-jre
WORKDIR /app
COPY mock-generator/target/mock-generator-0.0.1-SNAPSHOT-jar-with-dependencies.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar", "http://localhost:8080/api/v1/events", "200"]
