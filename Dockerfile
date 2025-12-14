FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY service/pom.xml service/pom.xml
COPY service/src service/src
COPY mock-generator/pom.xml mock-generator/pom.xml
COPY mock-generator/src mock-generator/src
RUN mvn -q -pl service -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/service/target/analytics-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
