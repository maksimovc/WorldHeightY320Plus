plugins {
    java
}

group = "dev.thenexusgates"
version = "1.0.0"

dependencies {
    compileOnly("com.hypixel.hytale:Server:+")
    implementation("org.ow2.asm:asm:9.8")
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith(".jar") }
            .map { zipTree(it) }
    })
}

apply(from = "gradle/hytale-micro-mod.gradle.kts")
