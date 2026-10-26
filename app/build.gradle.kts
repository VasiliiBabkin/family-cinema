import java.security.MessageDigest
plugins { id("com.google.devtools.ksp"); id("com.android.application"); kotlin("android"); id("org.jetbrains.kotlin.plugin.compose") }
android { namespace = "ru.familycinema.app"; compileSdk = 35
 defaultConfig { minSdk = 26; applicationId = "ru.familycinema.app"; targetSdk = 35; versionCode = 3; versionName = "0.3.0"; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_21; targetCompatibility = JavaVersion.VERSION_21 }
 bundle { language { enableSplit = false } }
 buildFeatures { compose = true }; sourceSets["main"].java.srcDir(layout.buildDirectory.dir("generated/demo"))
}
dependencies {
 implementation(project(":domain"))
 implementation(project(":parental")); implementation(project(":player")); implementation(project(":sources"))
 implementation(platform("androidx.compose:compose-bom:2024.12.01"))
 implementation("androidx.activity:activity-compose:1.9.3")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.foundation:foundation")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
 implementation("androidx.compose.material:material-icons-extended")
 
 testImplementation("junit:junit:4.13.2")
}

val generateDemo by tasks.registering {
 val asset = rootProject.file("app/src/main/assets/quiet-evening.mp4")
 val output = layout.buildDirectory.file("generated/demo/ru/familycinema/app/GeneratedDemo.kt")
 outputs.file(output)
 // Генерируем перед каждой сборкой: хеш должен соответствовать именно упакованным байтам.
 outputs.upToDateWhen { false }
 doLast {
  if (!asset.exists()) exec { commandLine("bash", rootProject.file("scripts/generate-demo.sh")) }
  val hash = MessageDigest.getInstance("SHA-256").digest(asset.readBytes()).joinToString("") { "%02x".format(it) }
  output.get().asFile.apply { parentFile.mkdirs(); writeText("package ru.familycinema.app\nobject GeneratedDemo { const val sha256 = \"$hash\" }\n") }
 }
}
tasks.named("preBuild") { dependsOn(generateDemo) }
dependencies {
 androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
 androidTestImplementation("androidx.compose.ui:ui-test-junit4")
 androidTestImplementation("androidx.test.ext:junit:1.2.1")
 androidTestImplementation("androidx.test:runner:1.6.2")
 androidTestImplementation("androidx.test.espresso:espresso-core:3.5.0")
 debugImplementation("androidx.compose.ui:ui-test-manifest")
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
 implementation("androidx.room:room-runtime:2.7.2")
 implementation("androidx.room:room-ktx:2.7.2")
 ksp("androidx.room:room-compiler:2.7.2")
 implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
 implementation("io.coil-kt:coil-compose:2.7.0")
 androidTestImplementation("androidx.room:room-testing:2.7.2")
}

android { sourceSets["androidTest"].assets.srcDir("schemas"); sourceSets["androidTest"].assets.srcDir("src/main/assets") }

android { defaultConfig { if (providers.gradleProperty("arm64Only").isPresent) ndk { abiFilters += "arm64-v8a" } } }

// The explicit arm64Only delivery is not a ChromeOS build; the default build remains universal.
android { lint { disable += "ChromeOsAbiSupport" } }

val generateQaMedia by tasks.registering {
 dependsOn(generateDemo)
 val clip=layout.buildDirectory.file("generated/qa-media/short-evening.mp4")
 outputs.file(clip)
 doLast { clip.get().asFile.parentFile.mkdirs();exec {commandLine("ffmpeg","-y","-v","error","-i",rootProject.file("app/src/main/assets/quiet-evening.mp4"),"-t","5","-c","copy",clip.get().asFile)} }
}
android { sourceSets["androidTest"].assets.srcDir(layout.buildDirectory.dir("generated/qa-media")) }
tasks.matching {it.name=="preDebugAndroidTestBuild"}.configureEach {dependsOn(generateQaMedia)}
