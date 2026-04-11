plugins {
    id("org.springframework.boot") version "4.0.5" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("gg.jte.gradle") version("3.2.3") apply false
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    dependencies {
        // todo: implementation() syntax?
        add("developmentOnly", "org.springframework.boot:spring-boot-devtools")
    }

    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.ai:spring-ai-bom:2.0.0-M4")
        }
    }
}
