import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.9.0"
    id("com.diffplug.spotless") version "6.25.0"
}

group = "com.vahid.plugin"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

spotless {
    java {
        licenseHeaderFile("config/license-header.txt")
        target("src/main/java/**/*.java", "src/test/java/**/*.java")
    }
}

dependencies {
    intellijPlatform {
        create("IC", "2025.1")
        bundledPlugins(listOf("Git4Idea"))
        testFramework(TestFrameworkType.Platform)
    }

    // For testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.2")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251"
            untilBuild = provider { null }
        }
    }

    pluginVerification {
        ides {
            create("IC", "2025.1")
        }
    }
}

tasks {
    withType<JavaCompile> {
        options.release.set(21)
        options.compilerArgs.add("-Xlint:deprecation")
    }

    test {
        useJUnitPlatform()
        jvmArgs("-ea")
    }

    runIde {
        jvmArgs("-ea")
    }
}
