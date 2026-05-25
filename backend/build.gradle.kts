import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort

plugins {
    java
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
    checkstyle
    id("com.github.spotbugs") version "6.0.22"
    jacoco
}

group = "dev.taskflow"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // OpenAPI / Swagger UI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    // Structured JSON logging
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    // Micrometer Prometheus registry
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.apache.httpcomponents.client5:httpclient5")
}

dependencyManagement {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:1.20.3")
    }
}

// ── Tests ─────────────────────────────────────────────────────────────────────

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading")
}

// ── JaCoCo ───────────────────────────────────────────────────────────────────

jacoco {
    toolVersion = "0.8.12"
}

val jacocoExcludes = listOf(
    // Boot entry point
    "**/TaskFlowApplication.class",
    // Infrastructure plumbing — config, entities, mappers, adapters (thin JPA wrappers)
    "**/infrastructure/config/**",
    "**/infrastructure/persistence/entity/**",
    "**/infrastructure/persistence/mapper/**",
    "**/infrastructure/persistence/adapter/**",
    "**/*MapperImpl.class",
    // DTO records — data containers, no business logic
    "**/application/dto/**",
    // Domain events — value objects, no branches to cover
    "**/domain/event/**",
    // Exception classes — constructors only, not meaningful to enforce
    "**/domain/exception/**",
)

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) { exclude(jacocoExcludes) }
        })
    )
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) { exclude(jacocoExcludes) }
        })
    )
    violationRules {
        rule {
            element = "CLASS"
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.70".toBigDecimal()
            }
        }
    }
}

// ── Checkstyle ────────────────────────────────────────────────────────────────

checkstyle {
    toolVersion = "10.18.2"
    configFile = file("checkstyle/checkstyle.xml")
    isIgnoreFailures = false
}

tasks.withType<Checkstyle> {
    reports {
        xml.required = true
        html.required = true
    }
}

// ── SpotBugs ──────────────────────────────────────────────────────────────────

spotbugs {
    effort = Effort.MAX
    reportLevel = Confidence.MEDIUM
    ignoreFailures = false
    excludeFilter = file("checkstyle/spotbugs-exclude.xml")
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask> {
    reports.create("html") {
        required = true
    }
    reports.create("xml") {
        required = false
    }
}
