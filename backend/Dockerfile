# Etapa 1: compilación
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# Etapa 2: ejecución
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 sigr && mkdir -p /app/data/imagenes && chown -R sigr /app/data
COPY --from=build /app/target/sigr-backend-1.0.0.jar app.jar
ENV SIGR_IMAGENES_DIR=/app/data/imagenes
USER sigr
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
