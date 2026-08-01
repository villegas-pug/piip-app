import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    java
    id("org.springframework.boot") version "4.1.0"
}

group = "pe.gob.midagri"
version = "1.0.0"
description = "Monolito modular para iniciativas y proyectos de innovación pública"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.1.0"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1")

    runtimeOnly("com.oracle.database.jdbc:ojdbc11:23.26.2.0.0")
    runtimeOnly("com.oracle.database.security:oraclepki:23.26.2.0.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:testcontainers-oracle-xe")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("com.h2database:h2")
}

layout.buildDirectory.set(layout.projectDirectory.dir("target"))

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("integration")
    }
}

val testSourceSet = sourceSets.named("test")

val integrationTest by tasks.registering(Test::class) {
    group = "verification"
    description = "Ejecuta únicamente las pruebas etiquetadas como integration."
    testClassesDirs = testSourceSet.get().output.classesDirs
    classpath = testSourceSet.get().runtimeClasspath
    shouldRunAfter(tasks.named("test"))
    useJUnitPlatform {
        includeTags("integration")
    }
}
