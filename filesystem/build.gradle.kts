import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.dokka)
    id("module.publication")
}

kotlin {
    explicitApi()

    jvm()
    androidTarget {
        publishLibraryVariants("release")
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions { jvmTarget = JvmTarget.JVM_1_8 }
    }
    @OptIn(ExperimentalWasmDsl::class) wasmJs { browser() }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.io.core)
            }
        }
        val commonTest by getting { dependencies { implementation(libs.kotlin.test) } }
        val jvmCommonMain by creating { dependsOn(commonMain) }
        val jvmMain by getting { dependsOn(jvmCommonMain) }
        val androidMain by getting { dependsOn(jvmCommonMain) }
        val nonJvmCommonMain by creating { dependsOn(commonMain) }
        val wasmJsMain by getting { dependsOn(nonJvmCommonMain) }
    }
}

android {
    namespace = "me.zhanghai.kotlin.filesystem"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies { coreLibraryDesugaring(libs.android.desugarJdkLibsNio) }
