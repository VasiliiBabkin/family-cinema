plugins { id("com.android.library"); kotlin("android") }
android { namespace = "ru.familycinema.parental"; compileSdk = 35
 defaultConfig { minSdk = 26;  }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_21; targetCompatibility = JavaVersion.VERSION_21 }
 
}
dependencies {
 implementation(project(":domain"))
 
 
 testImplementation("junit:junit:4.13.2")
}
