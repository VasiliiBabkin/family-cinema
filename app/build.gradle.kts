import java.security.MessageDigest
plugins { id("com.android.application"); kotlin("android"); id("org.jetbrains.kotlin.plugin.compose") }
android { namespace = "ru.familycinema.app"; compileSdk = 35
 defaultConfig { minSdk = 26; applicationId = "ru.familycinema.app"; targetSdk = 35; versionCode = 2; versionName = "0.2.0"; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
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
 debugImplementation("androidx.compose.ui:ui-test-manifest")
}
