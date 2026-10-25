plugins { kotlin("jvm") }
kotlin { jvmToolchain(21) }
dependencies {
    implementation(project(":domain"))
    api("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}
