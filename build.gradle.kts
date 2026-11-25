plugins {
    id("com.skillsjars.gradle-plugin") version "0.2.0"
    id("org.springframework.boot") version "4.2.0" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("gg.jte.gradle") version("3.2.5") apply false
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
            mavenBom("org.springframework.ai:spring-ai-bom:2.1.0")
        }
    }
}

// Agent Skills, extracted with ./gradlew extractSkillsJars
dependencies {
    skill("com.jamesward:skills:0.0.27")
}

skillsjars {
    outputDir.set(layout.projectDirectory.dir(".kiro/skills"))
}
