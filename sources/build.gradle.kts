plugins { kotlin("jvm") }
kotlin { jvmToolchain(21) }
dependencies { implementation(project(":domain")); testImplementation("junit:junit:4.13.2") }
