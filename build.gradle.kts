plugins {
    id("java")
    id("groovy")
    id("jacoco")
}

group = "com.ubiquiti.assignment"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

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

tasks.check {
    finalizedBy(tasks.jacocoTestReport)
}

