plugins {
    java
    id("io.freefair.lombok") version "6.5.0.3"
    application
    jacoco
    id("com.google.osdetector") version "1.7.0"
    id("org.panteleyev.jpackageplugin") version "1.3.1"
}

group = "com.github.naton1"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://maven.quiltmc.org/repository/release/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

application {
    mainClass.set("com.github.naton1.jvmexplorer.Startup")
}

val asmVersion = "9.3"
val javaFxVersion = "18.0.1"
val platformOverride = System.getenv("PLATFORM_OVERRIDE")
val javaFxPlatform = if (platformOverride != null) JavaFXPlatform.detect(platformOverride).classifier else JavaFXPlatform.detect(project).classifier

dependencies {
    implementation(project(":protocol"))

    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("ch.qos.logback:logback-core:1.2.11")
    implementation("ch.qos.logback:logback-classic:1.2.11")

    implementation("com.esotericsoftware:kryonet:2.22.0-RC1")
    implementation("org.quiltmc:quiltflower:1.8.1")
    implementation("org.fxmisc.richtext:richtextfx:0.10.9")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("org.hildan.fxgson:fx-gson:4.0.1") {
        // This package includes all javafx dependencies for some reason...
        exclude("org.openjfx")
    }

    implementation("org.ow2.asm:asm:${asmVersion}")
    implementation("org.ow2.asm:asm-util:${asmVersion}")

    implementation("org.openjdk.asmtools:asmtools-core:7.0.b10-ea")

    implementation("org.openjfx:javafx-base:${javaFxVersion}:${javaFxPlatform}")
    implementation("org.openjfx:javafx-controls:${javaFxVersion}:${javaFxPlatform}")
    implementation("org.openjfx:javafx-graphics:${javaFxVersion}:${javaFxPlatform}")
    implementation("org.openjfx:javafx-fxml:${javaFxVersion}:${javaFxPlatform}")

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
    testImplementation("org.testfx:testfx-junit5:4.0.16-alpha")
    testImplementation("org.mockito:mockito-core:4.6.1")
    testImplementation("org.mockito:mockito-junit-jupiter:4.6.1")

    // Allow screenshots
    testImplementation("org.openjfx:javafx-swing:${javaFxVersion}:${javaFxPlatform}")
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
        if (platformOverride != null) {
            archiveClassifier.set(platformOverride)
        }
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
    jpackage {
        dependsOn(jar)
        input = jar.get().archiveFile.get().asFile.parent
        appName = "JVM Explorer"
        vendor = "com.github.naton1"
        mainJar = jar.get().archiveFileName.get()
        mainClass = application.mainClass.get()
        javaOptions = listOf()
        destination = "$buildDir/jpackage"
        windows {
            winDirChooser = true
            winShortcut = true
        }
        linux {
            linuxShortcut = true
        }
    }
}

// Based on https://github.com/openjfx/javafx-gradle-plugin/blob/master/src/main/java/org/openjfx/gradle/JavaFXPlatform.java
// but they don't support 32bit windows
enum class JavaFXPlatform(val classifier: String, private val osDetectorClassifier: String) {

    LINUX("linux", "linux-x86_64"),
    LINUX_AARCH64("linux-aarch64", "linux-aarch_64"),
    WINDOWS("win", "windows-x86_64"),
    WINDOWS_X86("win-x86", "windows-x86_32"),
    OSX("mac", "osx-x86_64"),
    OSX_AARCH64("mac-aarch64", "osx-aarch_64"),
    ;

    companion object {
        fun detect(osClassifier: String): JavaFXPlatform {
            for (platform: JavaFXPlatform in values()) {
                if (platform.osDetectorClassifier == osClassifier) {
                    return platform
                }
            }
            throw GradleException("Unsupported JavaFX platform found: $osClassifier")
        }

        fun detect(project: Project): JavaFXPlatform {
            val osClassifier: String = project.extensions.getByType(com.google.gradle.osdetector.OsDetector::class.java).getClassifier()
            return detect(osClassifier)
        }
    }

}