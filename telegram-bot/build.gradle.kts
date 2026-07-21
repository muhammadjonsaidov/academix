plugins {
	java
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
	id("com.diffplug.spotless") version "7.0.4"
}

group = "uz.academixai"
version = "0.0.1-SNAPSHOT"
description = "AcademiX Telegram bot — standalone long-polling service, separate from backend/"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// No spring-boot-starter-webmvc pulled in for a full REST API — this service has no inbound
	// HTTP endpoints (long-polling is outbound-only). starter-web still needed for a minimal
	// /actuator/health for docker-compose healthchecks, matching backend's own pattern.
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-jdbc")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-amqp")
	// RestClient.Builder autoconfig doesn't come from starter-web alone — confirmed real in
	// backend/build.gradle.kts's own comment (a real startup failure without this).
	implementation("org.springframework.boot:spring-boot-starter-restclient")
	implementation("org.springframework.boot:spring-boot-starter-jackson")
	// spring-amqp's Jackson2JsonMessageConverter needs the legacy Jackson 2 databind package —
	// unlike backend/, nothing here pulls it in transitively (backend gets it incidentally via
	// springdoc-openapi/jackson-dataformat-yaml, confirmed in CLAUDE.md; this service has neither),
	// confirmed by a real "package com.fasterxml.jackson.databind does not exist" compile failure.
	implementation("com.fasterxml.jackson.core:jackson-databind:2.21.4")
	runtimeOnly("org.postgresql:postgresql")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// Same real gap backend/build.gradle.kts had and fixed — `./gradlew bootRun` does not load a
// .env file on its own, no dotenv library on the classpath. Reusing that exact fix here.
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
	val envFile = file(".env")
	if (envFile.exists()) {
		envFile.readLines()
			.map { it.trim() }
			.filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
			.forEach { line ->
				val key = line.substringBefore("=").trim()
				val value = line.substringAfter("=").trim()
				environment(key, value)
			}
	}
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
