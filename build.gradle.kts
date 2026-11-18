plugins {
    java
    jacoco
    idea
    kotlin("jvm")
    kotlin("plugin.spring")
    id("maven-publish")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("org.openapi.generator")
}

val versionKotlin: String by project
val versionSpringBoot: String by project
val versionDependencyManagement: String by project
val versionPostgresql: String by project
val versionJava: String by project
val versionProject: String by project
val projectGroup: String? by project

val versionOpenApiGenerator: String by project

// Testcontainers 1.21.4+ нужен для Docker Engine 29+ (в 1.19.x из BOM Spring Boot 3.2 подключение к Docker падает)
extra["testcontainers.version"] = "1.21.4"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:$versionSpringBoot")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Testcontainers: настоящая PostgreSQL в Docker для интеграционных тестов
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    implementation(kotlin("stdlib-jdk8"))

    // Jackson
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.openapitools:jackson-databind-nullable:0.2.6")

    // Bean Validation API support
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // SpringDoc dependencies
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    // Swagger annotations
    implementation("io.swagger.core.v3:swagger-annotations:2.2.8")

    // PostgreSQL
    implementation("org.postgresql:postgresql:${versionPostgresql}")

    // Spring Data JPA
    implementation("org.springframework.data:spring-data-jpa")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")

    // ModelMapper
    implementation("org.modelmapper:modelmapper:3.2.6")

}

tasks.bootJar {
    enabled = true
}

tasks.jar {
    enabled = false
}

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set("$rootDir/src/main/resources/openapi.yaml")
    outputDir.set("$buildDir/openapi")
    apiPackage.set("$projectGroup.controller")
    modelPackage.set("$projectGroup.dto")
    configOptions.set(
        mapOf(
            "dateLibrary" to "java8",
            "interfaceOnly" to "true",
            "useJakartaEe" to "true"
        )
    )
}

sourceSets.main {
    java.srcDir("$buildDir/openapi/src/main/java")
}

tasks.named("compileKotlin") {
    dependsOn("openApiGenerate")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports { html.required.set(true) }
}

// Быстрый прогон без Docker: ./gradlew unitTest
tasks.register<Test>("unitTest") {
    group = "verification"
    description = "Тесты без Docker (исключает тесты с тегом docker)"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        excludeTags("docker")
    }
}
