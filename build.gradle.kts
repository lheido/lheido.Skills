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

tasks.test {
    useJUnitPlatform()
}

tasks.register<Copy>("copyToMods") {
    dependsOn(tasks.named("jar"))
    from(tasks.named<Jar>("jar").map { it.outputs.files.singleFile })
    into("/var/home/lheido/.var/app/com.hypixel.HytaleLauncher/data/Hytale/UserData/Mods")
}

tasks.build {
    dependsOn("copyToMods")
}