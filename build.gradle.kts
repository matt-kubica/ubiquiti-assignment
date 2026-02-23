buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("info.solidsoft.gradle.pitest:gradle-pitest-plugin:1.15.0")
    }
}

plugins {
    id("java")
    id("groovy")
    id("jacoco")
    id("info.solidsoft.pitest") version "1.15.0"
}

group = "com.ubiquiti.assignment"
version = "1.0.0"



repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    implementation("jakarta.annotation:jakarta.annotation-api:3.0.0")
    implementation("io.vavr:vavr:0.10.6")

    testImplementation("org.spockframework:spock-core:2.4-M5-groovy-4.0")
    testImplementation("org.apache.groovy:groovy-all:4.0.26")
    testImplementation("org.mockito:mockito-core:5.16.1")
}


tasks.withType<Test> {
    useJUnitPlatform()
}

jacoco {
    toolVersion = "0.8.11"
}

pitest {
    pitestVersion = "1.15.0"
    junit5PluginVersion = "1.2.0"
    mutators = listOf("ALL")
}

tasks.check {
    finalizedBy(tasks.jacocoTestReport)
    finalizedBy(tasks.pitest)
}

tasks.pitest {
    mustRunAfter(tasks.jacocoTestReport)
}
