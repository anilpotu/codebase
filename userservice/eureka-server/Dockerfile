FROM maven:3.9.9-eclipse-temurin-11 AS build
WORKDIR /app

# Copy only required Maven descriptors first for better cache reuse
COPY pom.xml pom.xml
COPY eureka-server/pom.xml eureka-server/pom.xml

# Copy source and build only eureka-server module
COPY eureka-server/src eureka-server/src
RUN mvn -B -q -f eureka-server/pom.xml clean package -DskipTests

FROM eclipse-temurin:11-jre-alpine
WORKDIR /
VOLUME /tmp
COPY --from=build /app/eureka-server/target/*.jar app.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Xms256m", "-Xmx512m", "-jar", "/app.jar"]
EXPOSE 8761
