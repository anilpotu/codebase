FROM maven:3.9.9-eclipse-temurin-11 AS build
WORKDIR /app

COPY pom.xml pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY common-lib/pom.xml common-lib/pom.xml
COPY common-lib/src common-lib/src
COPY auth-service/src auth-service/src
RUN mvn -B -q -N -f pom.xml install
RUN mvn -B -q -f common-lib/pom.xml clean install -DskipTests
RUN mvn -B -q -f auth-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:11-jre-alpine
WORKDIR /
VOLUME /tmp
COPY --from=build /app/auth-service/target/*.jar app.jar
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Xms256m", "-Xmx512m", "-jar", "/app.jar"]
EXPOSE 8080
