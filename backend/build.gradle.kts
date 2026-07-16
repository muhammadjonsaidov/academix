plugins {
	java
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
	id("com.diffplug.spotless") version "7.0.4"
}

group = "uz.academixai"
version = "0.0.1-SNAPSHOT"
description = "AcademiX AI backend"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
	// jasperreports-pdf 7.0.7 depends on a Jaspersoft-forked OpenPDF artifact
	// (com.github.librepdf:openpdf:1.3.43.jaspersoft.1) not published to Maven Central —
	// confirmed by a real "Could not find ... .jaspersoft.1" resolution failure, and confirmed
	// this repo actually serves it (real HTTP 200, following the JFrog redirect).
	maven { url = uri("https://jaspersoft.jfrog.io/jaspersoft/third-party-ce-artifacts/") }
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-amqp")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	// Boot 4's fully modular starter split doesn't pull RestClient autoconfig in via webmvc —
	// confirmed by a real startup failure ("No qualifying bean of type RestClient.Builder")
	// with only spring-boot-starter-webmvc on the classpath. Needed for GoogleVisionClient/
	// QwenAIClient's outbound HTTP calls.
	implementation("org.springframework.boot:spring-boot-starter-restclient")
	// Same story for a shared ObjectMapper bean — webmvc's own Jackson message converter builds
	// one internally without exposing it, confirmed by a real startup failure the moment
	// QwenAIClient tried to inject ObjectMapper directly (needed to parse Qwen's JSON response).
	implementation("org.springframework.boot:spring-boot-starter-jackson")
	implementation("org.flywaydb:flyway-database-postgresql")
	runtimeOnly("org.postgresql:postgresql")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

	// Auth: self-issued JWTs (jjwt), not spring-security-oauth2 — see CLAUDE.md "Supporting tooling"
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

	// Rate limiting: Bucket4j + Redis (distributed) — Resilience4j stays scoped to circuit-breaker only
	implementation("com.bucket4j:bucket4j-core:8.10.1")
	implementation("com.bucket4j:bucket4j-redis:8.10.1")

	// Circuit breaker on Qwen/Vision calls
	implementation("io.github.resilience4j:resilience4j-spring-boot3:2.3.0")

	// API docs (reference aid, not codegen — see CLAUDE.md known gaps)
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")

	// Handwriting biometrics: pgvector JDBC support
	implementation("com.pgvector:pgvector:0.1.6")

	// Bulk import: .xlsx parsing (POST /admin/students/bulk-import/*)
	implementation("org.apache.poi:poi-ooxml:5.5.1")

	// Semester report PDFs (POST /admin/reports/generate) — LGPL, chosen over iText for licensing
	// reasons (academix_backend_tdd.md §"Supporting tooling"). Confirmed real on Maven Central.
	// jasperreports-pdf is a SEPARATE required module in 7.x (PDF export was split out of the core
	// artifact) — confirmed by a real JRRuntimeException ("Missing JasperReports PDF Extension")
	// when only the core jar was on the classpath.
	implementation("net.sf.jasperreports:jasperreports:7.0.7")
	implementation("net.sf.jasperreports:jasperreports-pdf:7.0.7")

	// SeaweedFS is S3-compatible — plain AWS SDK v2 S3 client pointed at its endpoint, no
	// SeaweedFS-specific SDK needed (see CLAUDE.md "Reality checks" for why SeaweedFS over MinIO)
	implementation(platform("software.amazon.awssdk:bom:2.47.6"))
	implementation("software.amazon.awssdk:s3")

	// homework.submissions.queue retry (3 attempts -> DLX, backend_tdd.md §6.3) — spring-retry is
	// present transitively already but not on the compile classpath until declared directly.
	// No separate "spring-boot-starter-aop" — confirmed real: it was discontinued after the 4.0.0-M2
	// milestone (no 4.0/4.1 GA release exists on Maven Central at all). AOP proxying already works
	// without it (resilience4j's @CircuitBreaker already proxies beans via spring-context's own
	// transitive spring-aop, same as @Transactional does) — this interceptor uses that same machinery.
	implementation("org.springframework.retry:spring-retry:2.0.13")

	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-amqp-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
	testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:testcontainers-junit-jupiter")
	testImplementation("org.testcontainers:testcontainers-postgresql")
	testImplementation("org.testcontainers:testcontainers-rabbitmq")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

spotless {
	java {
		googleJavaFormat()
		target("src/**/*.java")
	}
	kotlinGradle {
		target("*.gradle.kts")
	}
}
