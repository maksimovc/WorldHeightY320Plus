import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.Copy
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.register

apply(plugin = "java")

val deployFolderName = "earlyplugins"
val deployDir = rootProject.layout.buildDirectory.dir(deployFolderName)

repositories {
    mavenCentral()
    maven(url = "https://maven.hytale.com/release")
}

extensions.configure<JavaPluginExtension> {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withSourcesJar()
}

dependencies {
    add("compileOnly", "com.hypixel.hytale:Server:+")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.named<Copy>("processResources") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named<Jar>("jar") {
    archiveBaseName.set(project.name)
    manifest.attributes(
        mapOf(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version.toString()
        )
    )
}

val deployBuiltJar = tasks.register<Copy>("deployBuiltJar") {
    dependsOn(tasks.named("jar"))
    from(tasks.named<Jar>("jar").flatMap { it.archiveFile })
    into(deployDir)
}

tasks.named("build") {
    finalizedBy(deployBuiltJar)
}
