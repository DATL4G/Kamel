import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    alias(libs.plugins.org.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.org.jetbrains.compose)
    alias(libs.plugins.com.android.library)
    `maven-publish`
    signing
}

android {
    compileSdk = 33

    defaultConfig {
        minSdk = 21
        multiDexEnabled = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

}

kotlin {

    explicitApi = ExplicitApiMode.Warning

    android {
        publishAllLibraryVariants()
    }
    jvm("desktop")
    js(IR) {
        browser()
    }
    for (target in Targets.nativeTargets) {
        targets.add(presets.getByName(target).createTarget(target))
    }

    sourceSets {

        val commonMain by getting {
            dependencies {
                api(project(":kamel-core"))
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.runtime)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(project(":kamel-tests"))
                implementation(kotlin("test"))
                implementation(Dependencies.Testing.Ktor)
                implementation(Dependencies.Coroutines.Test)
            }
        }

        val jvmMain by creating {
            dependsOn(commonMain)
        }

        val jvmTest by creating {
            dependsOn(commonTest)
            dependencies {
                implementation(compose.material)
                implementation(Dependencies.Testing.Compose)
            }
        }

        val desktopMain by getting {
            dependsOn(jvmMain)
        }

        val desktopTest by getting {
            dependsOn(jvmTest)
            dependencies {
                implementation(Dependencies.Ktor.CIO)
                implementation(compose.desktop.currentOs)
            }
        }

        val androidMain by getting {
            dependsOn(jvmMain)
        }

        val androidUnitTest by getting {
            dependsOn(jvmTest)
        }

        val nonJvmMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(Dependencies.XmlUtil.Serialization)
            }
        }

        val nonJvmTest by creating {
            dependsOn(commonTest)
        }

        val jsMain by getting {
            dependsOn(nonJvmMain)
            dependencies {
                implementation(Dependencies.Ktor.Js)
            }
        }

        val darwinMain by creating {
            dependsOn(nonJvmMain)
            dependencies {
                implementation(Dependencies.Ktor.Darwin)
            }
        }

        val darwinTest by creating {
            dependsOn(nonJvmTest)
        }

        Targets.darwinTargets.forEach { target ->
            getByName("${target}Main") {
                dependsOn(darwinMain)
            }
            getByName("${target}Test") {
                dependsOn(darwinTest)
            }
        }

        all {
            languageSettings.apply {
                optIn("kotlin.Experimental")
            }
        }

    }
}

// https://youtrack.jetbrains.com/issue/KT-46466
val dependsOnTasks = mutableListOf<String>()
tasks.withType<AbstractPublishToMaven>().configureEach {
    dependsOnTasks.add(this.name.replace("publish", "sign").replaceAfter("Publication", ""))
    dependsOn(dependsOnTasks)
}