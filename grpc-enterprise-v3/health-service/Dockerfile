FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /app
COPY pom.xml .
COPY user-grpc-service/pom.xml user-grpc-service/pom.xml
COPY financial-service/pom.xml financial-service/pom.xml
COPY health-service/pom.xml health-service/pom.xml
COPY social-service/pom.xml social-service/pom.xml
COPY health-service/src health-service/src
RUN mvn clean package -DskipTests -pl health-service -am

FROM eclipse-temurin:11-jre-alpine
WORKDIR /app
COPY --from=build /app/health-service/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
