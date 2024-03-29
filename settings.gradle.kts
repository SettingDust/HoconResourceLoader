pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.quiltmc.org/repository/release") { name = "Quilt" }
        maven("https://maven.minecraftforge.net/") { name = "Forge" }
        maven("https://repo.spongepowered.org/repository/maven-public/") { name = "Sponge" }
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.spongepowered.mixin") {
                useModule("org.spongepowered:mixingradle:${requested.version}")
            }
        }
    }
}

dependencyResolutionManagement.versionCatalogs.create("catalog") {
    // https://plugins.gradle.org/plugin/org.jetbrains.gradle.plugin.idea-ext
    plugin("idea-ext", "org.jetbrains.gradle.plugin.idea-ext").version("1.1.7")

    // https://plugins.gradle.org/plugin/org.jetbrains.kotlin.jvm
    val kotlin = "1.9.22"
    version("kotlin", kotlin)
    plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm").version(kotlin)
    plugin("kotlin-plugin-serialization", "org.jetbrains.kotlin.plugin.serialization")
        .version(kotlin)

    // https://plugins.gradle.org/plugin/com.diffplug.spotless
    plugin("spotless", "com.diffplug.spotless").version("6.23.3")

    // https://github.com/jmongard/Git.SemVersioning.Gradle
    plugin("semver", "com.github.jmongard.git-semver-plugin").version("0.11.0")

    // https://fabricmc.net/develop/
    plugin("fabric-loom", "fabric-loom").version("1.5.+")

    val minecraft = "1.20.1"
    version("minecraft", minecraft)
    library("minecraft", "com.mojang", "minecraft").version(minecraft)

    library("yarn", "net.fabricmc", "yarn").version("$minecraft+build.10")

    library("fabric-loader", "net.fabricmc", "fabric-loader").version("0.15.3")

    val fabricApi = "0.91.0"
    library("fabric-api", "net.fabricmc.fabric-api", "fabric-api").version("$fabricApi+$minecraft")

    // https://modrinth.com/mod/fabric-language-kotlin/versions
    library(
            "fabric-kotlin",
            "net.fabricmc",
            "fabric-language-kotlin",
        )
        .version("1.10.17+kotlin.$kotlin")

    // https://modrinth.com/mod/modmenu/versions
    library("modmenu", "com.terraformersmc", "modmenu").version("7.2.2")

    /**
     * ***********
     * Quilt
     * ************
     */
    // https://github.com/QuiltMC/quilt-loom
    // Using 1.2 for https://github.com/QuiltMC/quilt-loom/issues/38
    plugin("quilt-loom", "org.quiltmc.loom").version("1.2.+")

    // https://github.com/QuiltMC/quilt-loader/tags
    library(
            "quilt-loader",
            "org.quiltmc",
            "quilt-loader",
        )
        .version("0.23.0-beta.1")

    // https://modrinth.com/mod/qsl/versions
    library(
            "quilt-fabric-api",
            "org.quiltmc.quilted-fabric-api",
            "quilted-fabric-api",
        )
        .version("7.4.0+0.90.0-$minecraft")

    /**
     * ***********
     * Forge
     * ************
     */
    // https://maven.neoforged.net/#/releases/net/neoforged/gradle/userdev
    plugin("forge-gradle", "net.minecraftforge.gradle").version("6.+")

    // https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html
    library("forge", "net.minecraftforge", "forge").version("$minecraft-47.2.19")

    // https://github.com/SpongePowered/MixinGradle
    plugin("mixin-gradle", "org.spongepowered.mixin").version("0.7-SNAPSHOT")

    // https://modrinth.com/mod/connector/versions
    library("connector", "dev.su5ed.sinytra", "Connector").version("1.0.0-beta.32+$minecraft")
    // https://modrinth.com/mod/forgified-fabric-api/versions
    library("forgified-fabric-api", "dev.su5ed.sinytra.fabric-api", "fabric-api")
        .version("$fabricApi+1.10.5+$minecraft")
    // https://modrinth.com/mod/connector-extras/versions
    library("connector-extras", "maven.modrinth", "connector-extras").version("1.9.2+$minecraft")

    /**
     * ***********
     * Libraries
     * ************
     */
    // https://modrinth.com/mod/kinecraft-serialization/versions
    library("kinecraft-serialization", "maven.modrinth", "kinecraft-serialization")
        .version("1.3.0-fabric")

    // https://github.com/Kotlin/kotlinx.serialization
    library("kotlinx-serialization-hocon", "org.jetbrains.kotlinx", "kotlinx-serialization-hocon")
        .version("1.6.2")

    // https://github.com/lightbend/config
    library("typesafe-config", "com.typesafe", "config").version("1.4.3")

    // https://modrinth.com/mod/patched/versions
    library("patched", "maven.modrinth", "patched").version("3.2.3+$minecraft-fabric")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
    // https://github.com/DanySK/gradle-pre-commit-git-hooks
    id("org.danilopianini.gradle-pre-commit-git-hooks") version "2.0.1"
}

gitHooks {
    preCommit {
        from {
            //             git diff --cached --name-only --diff-filter=ACMR | while read -r a; do
            // echo ${'$'}(readlink -f ${"$"}a); ./gradlew spotlessApply -q
            // -PspotlessIdeHook="${'$'}(readlink -f ${"$"}a)" </dev/null; done
            """
            export JAVA_HOME="${System.getProperty("java.home")}"
            ./gradlew spotlessApply spotlessCheck
            """
                .trimIndent()
        }
    }
    commitMsg { conventionalCommits { defaultTypes() } }
    hook("post-commit") {
        from {
            """
            files="${'$'}(git show --pretty= --name-only | tr '\n' ' ')"
            git add ${'$'}files
            git -c core.hooksPath= commit --amend -C HEAD
            """
                .trimIndent()
        }
    }
    createHooks(true)
}

val name: String by settings

rootProject.name = name

include("mod")

include("quilt")

include("forge")
