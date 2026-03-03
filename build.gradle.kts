import java.util.jar.JarFile

plugins {
    id("java")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation(files("libs/HytaleServer.jar"))
}

val hytaleServerJar = file("libs/HytaleServer.jar")

val serverVersion: String by lazy {
    if (!hytaleServerJar.exists()) {
        throw GradleException("Missing ${hytaleServerJar.path}")
    }

    JarFile(hytaleServerJar).use { jar ->
        jar.manifest?.mainAttributes?.getValue("Implementation-Version")
            ?: throw GradleException("Implementation-Version not found in ${hytaleServerJar.path}")
    }
}

tasks.processResources {
    inputs.file(hytaleServerJar)
    inputs.property("serverVersion", serverVersion)

    filesMatching("manifest.json") {
        expand(mapOf("serverVersion" to serverVersion))
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.javadoc {
    options {
        (this as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
    }
}

tasks.register<Copy>("copyToMods") {
    dependsOn(tasks.named("jar"))
    from(tasks.named<Jar>("jar").map { it.outputs.files.singleFile })
    into("/var/home/lheido/.var/app/com.hypixel.HytaleLauncher/data/Hytale/UserData/Mods")
}

tasks.build {
    dependsOn("copyToMods")
}
