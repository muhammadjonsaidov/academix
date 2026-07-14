# Multi-stage build per CLAUDE.md "Supporting tooling" — pure-JDBC pgvector client,
# no native deps needing glibc, so alpine is fine for the runtime stage.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN ./gradlew --no-daemon dependencies || true
COPY src src
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jre-alpine AS run
WORKDIR /app
RUN addgroup -S academix && adduser -S academix -G academix
COPY --from=build /app/build/libs/*.jar app.jar
USER academix
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
