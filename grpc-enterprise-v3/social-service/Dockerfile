FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /app
COPY pom.xml .
COPY social-service/pom.xml social-service/pom.xml
COPY social-service/src social-service/src
RUN mvn clean package -DskipTests -pl social-service -am

FROM eclipse-temurin:11-jre-alpine
WORKDIR /app
COPY --from=build /app/social-service/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
