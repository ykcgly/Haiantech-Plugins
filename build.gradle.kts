plugins {
    java
}

group = "com.haiman233"
version = "1.10.0-standalone"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.release.set(17)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    // 编译目标 = 测试服实际运行的中文版 Slimefun4（API 与官方版路径一致，行为核对过 registerMobDrop）
    compileOnly(files("libs/Slimefun-f172239-Beta.jar"))
    // 旧格式 saveditem 净化解析需要原生 SnakeYAML（运行时由 Paper 服务器提供 2.2）
    compileOnly("org.yaml:snakeyaml:2.2")
    // Slimefun jar 的类签名引用了 JetBrains 注解；缺失时 javac 在特定推断场景会崩溃
    compileOnly("org.jetbrains:annotations:26.0.2")
}

configurations.compileClasspath {
    attributes {
        attribute(org.gradle.api.attributes.java.TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
    }
}

// 把 content/ 下的 YAML 内容与 saveditems/ 一并打进 jar（插件运行期从自身资源读取）
tasks.processResources {
    filteringCharset = "UTF-8"
    from(rootProject.projectDir.resolve("content")) {
        include("*.yml")
        into("")
    }
    from(rootProject.projectDir.resolve("content/saveditems")) {
        include("*.yml")
        into("saveditems")
    }
}

tasks.jar {
    archiveBaseName.set("HaimanTech2")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.jar)
}
