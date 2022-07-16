plugins {
    java
    id("io.freefair.lombok") version "6.5.0.3"
}

group = "com.github.naton1"
version = "0.5.0"

java.sourceCompatibility = JavaVersion.VERSION_1_7
java.targetCompatibility = JavaVersion.VERSION_1_7

repositories {
    mavenCentral()
}

val asmVersion = "9.3"

dependencies {
    implementation("org.ow2.asm:asm:${asmVersion}")
    implementation("org.ow2.asm:asm-commons:${asmVersion}")
}

tasks {
    jar {
        manifest {
            attributes["Premain-Class"] = "com.github.naton1.jvmexplorer.agent.launch.LaunchPatchAgent"
            attributes["Can-Redefine-Classes"] = "true"
            attributes["Can-Retransform-Classes"] = "true"
        }
        duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
        destinationDirectory.set(file("$buildDir/libs/agents"))
        archiveFileName.set("${archiveBaseName.get()}.jar")
    }
}