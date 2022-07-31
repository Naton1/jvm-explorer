import org.openjfx.gradle.JavaFXPlatform

plugins {
    java
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("io.freefair.lombok") version "6.5.0.3"
    application
    jacoco
}

group = "com.github.naton1"
version = "0.7.0"

repositories {
    mavenCentral()
    maven("https://maven.quiltmc.org/repository/release/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

javafx {
    version = "18.0.1"
    modules = listOf("javafx.base", "javafx.graphics", "javafx.fxml", "javafx.controls")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

application {
    mainClass.set("com.github.naton1.jvmexplorer.Startup")
}

val asmVersion = "9.3"

dependencies {
    implementation(project(":protocol"))

    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("ch.qos.logback:logback-core:1.2.11")
    implementation("ch.qos.logback:logback-classic:1.2.11")

    implementation("com.esotericsoftware:kryonet:2.22.0-RC1")
    implementation("org.quiltmc:quiltflower:1.8.1")
    implementation("org.fxmisc.richtext:richtextfx:0.10.9")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("org.hildan.fxgson:fx-gson:4.0.1")

    implementation("org.ow2.asm:asm:${asmVersion}")
    implementation("org.ow2.asm:asm-util:${asmVersion}")

    implementation("com.roscopeco.jasm:jasm:0.6.0")

    // Let's build a fat jar for now... this includes the java runtime deps for windows/linux/mac
    implementation("org.openjfx:javafx-base:${javafx.version}:${JavaFXPlatform.WINDOWS.classifier}")
    implementation("org.openjfx:javafx-base:${javafx.version}:${JavaFXPlatform.LINUX.classifier}")
    implementation("org.openjfx:javafx-base:${javafx.version}:${JavaFXPlatform.OSX.classifier}")
    implementation("org.openjfx:javafx-controls:${javafx.version}:${JavaFXPlatform.WINDOWS.classifier}")
    implementation("org.openjfx:javafx-controls:${javafx.version}:${JavaFXPlatform.LINUX.classifier}")
    implementation("org.openjfx:javafx-controls:${javafx.version}:${JavaFXPlatform.OSX.classifier}")
    implementation("org.openjfx:javafx-graphics:${javafx.version}:${JavaFXPlatform.WINDOWS.classifier}")
    implementation("org.openjfx:javafx-graphics:${javafx.version}:${JavaFXPlatform.LINUX.classifier}")
    implementation("org.openjfx:javafx-graphics:${javafx.version}:${JavaFXPlatform.OSX.classifier}")
    implementation("org.openjfx:javafx-fxml:${javafx.version}:${JavaFXPlatform.WINDOWS.classifier}")
    implementation("org.openjfx:javafx-fxml:${javafx.version}:${JavaFXPlatform.LINUX.classifier}")
    implementation("org.openjfx:javafx-fxml:${javafx.version}:${JavaFXPlatform.OSX.classifier}")

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("org.testfx:testfx-junit5:4.0.16-alpha")
    testImplementation("org.mockito:mockito-core:3.+")
    testImplementation("org.mockito:mockito-junit-jupiter:3.12.4")

    // Allows screenshots
    testImplementation("org.openjfx:javafx-swing:${javafx.version}:${JavaFXPlatform.detect(project).classifier}")
}

sourceSets {
    main {
        java {
            resources {
                srcDir("../agent/build/libs")
                srcDir("../launch-agent/build/libs")
            }
        }
    }
    val integration by creating {
        compileClasspath += main.get().output + test.get().output
        runtimeClasspath += main.get().output + test.get().output
    }
}

configurations {
    val integrationImplementation by getting {
        extendsFrom(configurations.testImplementation.get())

        // This is a super hack. Try to find something better to add :agent as a dependency.
        withDependencies {
            val dependencySet = this
            dependencies {
                dependencySet.add(project(":agent"))
            }
        }
    }
}

tasks {
    test {
        useJUnitPlatform()
    }
    jar {
        into("agents") {
            from(configurations.runtimeClasspath.get().filter { it.name.contains("agent") })
        }
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
            attributes["Main-Class"] = application.mainClass.get()
            attributes["Implementation-Version"] = archiveVersion
        }
        from(configurations.runtimeClasspath.get().filter { !it.name.contains("agent") }.map {
            if (it.isDirectory) it
            else zipTree(it)
        })
        archiveBaseName.set("jvm-explorer")
    }
    processResources {
        dependsOn(":agent:jar")
        dependsOn(":launch-agent:jar")
    }
    test {
        finalizedBy(jacocoTestReport)
    }
    jacocoTestReport {
        dependsOn(test)
        classDirectories.setFrom(
                sourceSets.main.get().output.asFileTree.matching {
                    include("**/com/github/naton1/jvmexplorer/**")
                }
        )
    }
    val verifyJarStarts by creating {
        group = "verification"
        dependsOn(jar)
        doLast {
            val jarPath = jar.get().archiveFile.get().asFile.absolutePath
            val processBuilder = ProcessBuilder()
                    .command("java", "-jar", jarPath)
                    .inheritIO()
            val process = processBuilder.start()
            val processEnded = process.waitFor(10L, TimeUnit.SECONDS)
            if (processEnded) {
                throw GradleException("JAR process ended too soon")
            }
            process.destroy()
            val destroyed = process.waitFor(10L, TimeUnit.SECONDS)
            if (!destroyed) {
                process.destroyForcibly()
                throw GradleException("Process did not exit gracefully")
            }
        }
    }
    val integrationTest by creating(Test::class) {
        group = "verification"
        useJUnitPlatform()
        testClassesDirs = sourceSets.getByName("integration").output.classesDirs
        classpath = sourceSets.getByName("integration").runtimeClasspath
    }
}