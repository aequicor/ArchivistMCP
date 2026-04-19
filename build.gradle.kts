plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    application
}

group = "io.aeqiocor.archivistmcp"
version = "0.1.0"

application {
    mainClass.set("io.aeqiocor.archivistmcp.MainKt")
}

dependencies {
    implementation(dependencies.platform(libs.ktor.bom))
    implementation(libs.mcp.kotlin.server)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.cors)
    implementation(libs.slf4j.simple)
    implementation(libs.langchain4j)
    implementation(libs.langchain4j.embeddings)

    testImplementation(libs.mcp.kotlin.client)
    testImplementation(libs.ktor.client.cio)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        javaParameters = true
        freeCompilerArgs.addAll(
            "-Xdebug",
        )
    }
}
