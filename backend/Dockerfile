# --- build stage ---
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# copia primeiro só o necessário pra baixar dependências (aproveita cache do Docker)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline

COPY src src
RUN ./mvnw -q clean package -DskipTests

# --- run stage ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# pasta onde as fotos dos animais são salvas (ver bovix.uploads-dir)
RUN mkdir -p /app/uploads/animais

COPY --from=build /app/target/*.jar app.jar

# Render define a porta via variável PORT — repassamos pro Spring Boot no ENTRYPOINT abaixo
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8080}"]
