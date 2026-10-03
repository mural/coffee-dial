plugins {
    kotlin("multiplatform") version "2.4.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
    id("org.jetbrains.compose") version "1.12.1" apply false
    id("com.android.application") version "9.3.1" apply false
    id("com.android.kotlin.multiplatform.library") version "9.3.1" apply false
    id("app.cash.sqldelight") version "2.4.0" apply false
}

val ktlint by configurations.creating {
    attributes { attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.SHADOWED)) }
}
dependencies { ktlint("com.pinterest.ktlint:ktlint-cli:1.8.0") }
tasks.register<JavaExec>("ktlintCheck") {
    group = "verification"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args("**/src/**/*.kt", "**/*.gradle.kts", "!**/build/**", "!**/.gradle/**")
}
tasks.register<JavaExec>("ktlintFormat") {
    group = "formatting"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args("-F", "**/src/**/*.kt", "**/*.gradle.kts", "!**/build/**", "!**/.gradle/**")
}
