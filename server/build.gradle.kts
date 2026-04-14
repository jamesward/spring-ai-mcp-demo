plugins {
    id("gg.jte.gradle")
}

jte {
    generate()
    binaryStaticContent = true
    jteExtension("gg.jte.models.generator.ModelExtension")
}

dependencies {
    implementation("org.springframework.ai:spring-ai-starter-mcp-server-webflux")
    implementation("gg.jte:jte-runtime:3.2.3")
    jteGenerate("gg.jte:jte-models:3.2.3")

    implementation("org.webjars:webjars-locator-lite:1.1.3")
    runtimeOnly("org.webjars.npm:modelcontextprotocol__ext-apps:1.5.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
