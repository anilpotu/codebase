FROM eclipse-temurin:11-jre-alpine
VOLUME /tmp
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Xms256m", \
  "-Xmx512m", \
  "-jar", \
  "/app.jar"]
EXPOSE 8082
