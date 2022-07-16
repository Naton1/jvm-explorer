plugins {
    java
    id("io.freefair.lombok") version "5.2.1"
}

group = "com.github.naton1"
version = "0.5.0"

java.sourceCompatibility = JavaVersion.VERSION_1_7
java.targetCompatibility = JavaVersion.VERSION_1_7

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":protocol"))
    implementation("com.esotericsoftware:kryonet:2.22.0-RC1")
}

tasks {
    jar {
        manifest {
            attributes["Agent-Class"] = "com.github.naton1.jvmexplorer.agent.JvmExplorerAgent"
            attributes["Can-Redefine-Classes"] = "true"
            attributes["Can-Retransform-Classes"] = "true"
        }
        duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
        destinationDirectory.set(file("$buildDir/libs/agents"))
        archiveFileName.set("${archiveBaseName.get()}.jar")
    }
}