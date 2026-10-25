plugins { id("com.android.library"); kotlin("android") }
android { namespace = "ru.familycinema.player"; compileSdk = 35
 defaultConfig { minSdk = 26;  }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_21; targetCompatibility = JavaVersion.VERSION_21 }
 
}
dependencies {
 implementation(project(":domain"))
 
 api("androidx.media3:media3-exoplayer:1.5.1"); api("androidx.media3:media3-ui:1.5.1")
 testImplementation("junit:junit:4.13.2")
}
