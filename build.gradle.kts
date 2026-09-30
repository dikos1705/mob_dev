plugins {
    kotlin("jvm") version "2.2.20"
    application
}

repositories { mavenCentral() }

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.2")
}

kotlin { jvmToolchain(21) }
application { mainClass.set("library.MainKt") }
tasks.test { useJUnitPlatform() }
tasks.named<JavaExec>("run") { standardInput = System.`in` }
