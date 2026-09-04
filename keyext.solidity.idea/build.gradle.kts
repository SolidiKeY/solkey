plugins {
    // Has to be at least the Kotlin the *newest* target IDE ships, or its jars are unreadable:
    // IDEA 2026.2 carries 2.4.0 metadata, which a 2.1 compiler rejects outright.
    kotlin("jvm") version "2.4.0"
    id("org.jetbrains.intellij.platform") version "2.10.1"
}

group = "org.key_project.solidity"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Build against an IDE already on this machine instead of downloading one:
// -PidePath=/path/to/idea (the directory holding bin/ and lib/). Worth using before installing
// into an IDE much newer than the default below — it turns "probably compatible" into a compile.
val localIde: String? = providers.gradleProperty("idePath").orNull

dependencies {
    intellijPlatform {
        if (localIde != null) {
            local(localIde)
        } else {
            // IDEA Community; the plugin uses no Ultimate-only API and runs in both.
            intellijIdeaCommunity("2025.2")
        }
        // ExternalSystemUtil / GradleConstants, for launching :keyext.solidity.gui:solidityGui.
        bundledPlugin("com.intellij.gradle")
    }
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    // The platform plugin installs its own class loader and its test bootstrap reaches for JUnit 4,
    // so both the launcher and JUnit 4 have to be on the runtime classpath even though the tests
    // here are plain Jupiter ones that touch no IDE API.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            // 2025.1. Kept close to the platform built against rather than as low as the code
            // would probably run, because nothing here verifies an older one: verifyPlugin needs
            // an `ides { }` matrix, which means downloading another IDE per version claimed.
            sinceBuild = "251"
            // No upper bound. The default pins untilBuild to the IDE built against, which silently
            // disables the plugin on the next IDE upgrade — the same version-drift failure
            // docs/idea-setup.md documents for hardcoded module names in .run/ configurations.
            untilBuild = provider { null }
        }
    }
}

// Plugins for 2024.2+ must be Java 21 bytecode, whatever JBR the IDE itself runs on.
kotlin {
    jvmToolchain(21)
}

// Spawns a whole headless IDE to index the settings page for Settings search. That only matters
// for a Marketplace listing, and it is the one step of this build that needs a working display
// server, so it fails on CI machines and in containers.
tasks.buildSearchableOptions {
    enabled = false
}

// The sandbox IDE runs on the JBR bundled with the downloaded distribution, which is not patched
// for distributions that do not put the X libraries on the default loader path (NixOS is the one
// this was hit on: it fails with `libXext.so.6: cannot open shared object file`). Point this at a
// JDK 21+ that does work: -PideJdk=/path/to/jdk-home.
providers.gradleProperty("ideJdk").orNull?.let { home ->
    tasks.runIde {
        javaLauncher.set(null as org.gradle.jvm.toolchain.JavaLauncher?)
        executable(File(File(home, "bin"), "java").absolutePath)
    }
}

// Opens a project in the sandbox IDE: -PideProject=/path/to/dir. keyext.solidity.examples is the
// useful one — small enough to import instantly, and full of .sol files to see icons on.
providers.gradleProperty("ideProject").orNull?.let { path ->
    tasks.runIde {
        args(path)
    }
}

tasks.test {
    useJUnitPlatform()
    // The platform plugin's test bootstrap initialises AWT; nothing here needs a display.
    systemProperty("java.awt.headless", "true")
}
