# --- Etapa 1: compilación ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# --- Etapa 2: ejecución (imagen mínima, usuario sin privilegios) ---
FROM eclipse-temurin:21-jre
RUN useradd --system --no-create-home appuser
WORKDIR /app
COPY --from=build /app/target/auth-service-*.jar app.jar
USER appuser
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
