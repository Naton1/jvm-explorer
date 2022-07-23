plugins {
    java
    id("io.freefair.lombok") version "6.5.0.3"
    jacoco
}

group = "com.github.naton1"
version = "0.5.0"

java.sourceCompatibility = JavaVersion.VERSION_1_7
java.targetCompatibility = JavaVersion.VERSION_1_7

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("com.esotericsoftware:kryonet:2.22.0-RC1")
    testImplementation("junit:junit:4.13.2")
}

tasks {
    test {
        finalizedBy(jacocoTestReport)
    }
    jacocoTestReport {
        dependsOn(test)
    }
}