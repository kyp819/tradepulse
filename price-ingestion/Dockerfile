FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/price-ingestion-0.0.1-SNAPSHOT-exec.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
