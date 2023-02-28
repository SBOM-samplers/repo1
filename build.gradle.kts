import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    jacoco
    java
    id("org.springframework.boot") version "2.6.7"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("com.google.cloud.tools.jib") version "3.1.4"
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.spring") version "1.6.21"
    kotlin("kapt") version "1.6.21"
}

group = "com.trendyol.qa"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_15
java.targetCompatibility = JavaVersion.VERSION_15

repositories {
    mavenCentral()
    maven { url = uri("https://oss.jfrog.org/artifactory/oss-snapshot-local/") }
}

kapt {
    arguments {
        arg("mapstruct.defaultComponentModel", "spring")
    }
}

configurations {
    all {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    }
}

extra["springCloudVersion"] = "2021.0.2"

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-security:2.6.7")
    implementation("org.springframework.boot:spring-boot-starter-data-ldap")
    implementation("org.springdoc:springdoc-openapi-webflux-ui:1.6.8")
    implementation("org.springdoc:springdoc-openapi-ui:1.6.8")
    implementation("org.springdoc:springdoc-openapi-kotlin:1.6.8")
    implementation("com.google.code.gson:gson")

    // Spring Cloudweb
    implementation("org.springframework.cloud:spring-cloud-starter-config:3.0.5")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign:3.1.1")
    implementation("io.github.openfeign:feign-okhttp:10.2.0")
    implementation("org.springframework.cloud:spring-cloud-starter-bootstrap:3.0.4")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.5.2-native-mt")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.1.6")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.2")
    implementation("org.apache.logging.log4j:log4j-api:2.17.2")
    implementation("org.apache.logging.log4j:log4j-core:2.17.2")

    // Bean Mapper(MapStruct)
    implementation("org.mapstruct:mapstruct:1.4.2.Final")
    kapt("org.mapstruct:mapstruct-processor:1.4.2.Final")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test:2.6.7") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.springframework.security:spring-security-test:5.6.3")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.12.4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.2-native-mt")
    testImplementation("io.projectreactor:reactor-test:3.4.18")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

jib {
    container {
        ports = listOf("8082")
    }
}

jacoco {
    toolVersion = "0.8.8"
}

val coverageExcludedPackages = listOf(
    "com.trendyol.qa.aresgateway.AresGatewayApplication*"
)

tasks {
    jacocoTestReport {
        exclude(coverageExcludedPackages)
        dependsOn(test) // tests are required to run before generating the report
        finalizedBy(jacocoTestCoverageVerification) // verification is always run after report generated
        reports {
            xml.required.set(true)
            csv.required.set(true)
            html.required.set(true)
        }
    }

    jacocoTestCoverageVerification {
        exclude(coverageExcludedPackages)
        violationRules {
            rule {
                limit {
                    counter = "LINE"
                    minimum = BigDecimal.valueOf(0.7)
                }
            }
        }
    }

    withType<Test> {
        useJUnitPlatform()
        finalizedBy(jacocoTestReport)
    }

    withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict", "-opt-in=kotlin.RequiresOptIn")
            jvmTarget = "15"
        }
    }
}

@TaskAction
fun JacocoReportBase.exclude(excludedPackages: List<String>) {
    classDirectories.setFrom(classDirectories.files.map { file ->
        fileTree(file) {
            exclude(excludedPackages.map { it.replace('.', '/') })
        }
    })
}
