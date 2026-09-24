// JSON-контент (src/main/resources/content) и его загрузка
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    implementation(project(":core"))
    implementation(libs.kotlinx.serialization.json)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Симулятор баланса для Ярика: ./gradlew :content:simulate — таблицы трёх сценариев по настоящим JSON
tasks.register<JavaExec>("simulate") {
    group = "cashpet"
    description = "Баланс: 5 недель трёх сценариев по настоящему контенту, как в docs/02-экономика.md"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("io.github.chalexey.cashpet.content.SimulateKt")
    workingDir = projectDir
    // Кириллица в консоли Windows
    jvmArgs("-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8")
}
