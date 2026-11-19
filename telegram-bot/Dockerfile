# Same multi-stage shape as backend/Dockerfile — no native deps, alpine runtime is fine.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY src src
# The Gradle cache (wrapper + dependencies) lives in a BuildKit cache mount, so it survives across
# builds: a changed build.gradle.kts only downloads what is new instead of everything again.
RUN --mount=type=cache,id=gradle-telegram-bot,target=/root/.gradle ./gradlew --no-daemon --quiet bootJar

FROM eclipse-temurin:21-jre-alpine AS run
WORKDIR /app
RUN addgroup -S academix && adduser -S academix -G academix
COPY --from=build /app/build/libs/*.jar app.jar
USER academix
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
