FROM openjdk:21

WORKDIR /app

COPY target/domashna2-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8081

CMD ["java", "-jar", "app.jar"]

