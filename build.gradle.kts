
plugins {
    application
    java
}

group = "xyz.blackdev"
version = "v1"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(24))
    }
}

repositories {
    mavenCentral()
    maven {
        name = "craftsblockReleases"
        url = uri("https://repo.craftsblock.de/releases")
    }
}

dependencies {
    implementation("de.craftsblock:craftsnet:3.5.4")
    implementation("de.craftsblock.craftscore:all:3.8.11")
    implementation("org.slf4j:slf4j-simple:2.0.12")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.yaml:snakeyaml:2.2")
}

application {
    mainClass.set("xyz.blackdev.wgetapi.WgetAPI")
}
