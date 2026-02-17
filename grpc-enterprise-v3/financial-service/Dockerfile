FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /app
COPY pom.xml .
COPY financial-service/pom.xml financial-service/pom.xml
COPY financial-service/src financial-service/src
RUN mvn clean package -DskipTests -pl financial-service -am

FROM eclipse-temurin:11-jre-alpine
WORKDIR /app
COPY --from=build /app/financial-service/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
