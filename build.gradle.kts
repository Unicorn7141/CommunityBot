import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.10"
    application
}

group = "me.user"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven {
        name = "Kotlin Discord"
        url = uri("https://maven.kotlindiscord.com/repository/maven-public/")
    }
}

dependencies {
    implementation("org.slf4j:slf4j-simple:1.7.32")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.5.21")
    implementation("org.litote.kmongo:kmongo:4.2.8")
    implementation("org.litote.kmongo:kmongo-async:4.2.8")
    implementation("org.litote.kmongo:kmongo-coroutine:4.2.8")
    implementation("org.litote.kmongo:kmongo-reactor:4.2.8")
    implementation("org.litote.kmongo:kmongo-rxjava2:4.2.8")
    implementation("io.ktor:ktor-client-java:1.6.3")
    implementation("com.kotlindiscord.kord.extensions:kord-extensions:1.5.0-SNAPSHOT")

}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "9"
}

application {
    mainClass.set("MainKt")
}