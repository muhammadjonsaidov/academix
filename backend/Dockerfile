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

# JDK (not JRE) in the runtime stage, deliberately: JasperReportGenerator's @PostConstruct
# compiles quarter-report JRXML → .java → javac at startup, and a JRE has no javac — confirmed
# by a real deploy crash-loop ("Cannot run program \"javac\"") that local `bootRun` (full JDK)
# never surfaced. Costs ~190MB of image size; alternative (jasperreports-jdt in-process
# compiler) avoids that but adds a dependency — revisit if image size ever matters.
FROM eclipse-temurin:21-jdk-alpine AS run
WORKDIR /app
RUN addgroup -S academix && adduser -S academix -G academix
COPY --from=build /app/build/libs/*.jar app.jar
USER academix
EXPOSE 8080
# Small-container default: modest heap, SerialGC (right
# choice for small heaps — throughput GCs just add overhead here), capped metaspace/code
# cache/thread stacks. Realistic RSS ~500-650MB. Metaspace is 256m, NOT lower — a real hosted
# deploy OOM'd ("OutOfMemoryError: Metaspace") at 160m under live traffic: Jackson 3's generated
# (de)serializers, the JDT in-process JRXML compiler, and Spring Security observation classes
# together need well over 160m. Override by setting JAVA_TOOL_OPTIONS on the container env
# (the JVM picks it up automatically; an env-provided value replaces this one).
ENV JAVA_TOOL_OPTIONS="-Xmx288m -Xms128m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=64m -XX:MaxDirectMemorySize=64m -Xss512k -XX:+UseSerialGC"
ENTRYPOINT ["java", "-jar", "app.jar"]
