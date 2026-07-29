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
# Small-container default (Railway trial caps 0.5GB RAM/service): tight heap, SerialGC (right
# choice for sub-512MB heaps — throughput GCs just add overhead here), capped metaspace/code
# cache/thread stacks. Realistic RSS ~380-450MB. Override by setting JAVA_TOOL_OPTIONS on the
# container env (the JVM picks it up automatically; an env-provided value replaces this one).
ENV JAVA_TOOL_OPTIONS="-Xmx224m -Xms128m -XX:MaxMetaspaceSize=160m -XX:ReservedCodeCacheSize=48m -XX:MaxDirectMemorySize=64m -Xss512k -XX:+UseSerialGC"
ENTRYPOINT ["java", "-jar", "app.jar"]
