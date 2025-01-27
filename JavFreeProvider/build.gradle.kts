version = 8

cloudstream {
    // All of these properties are optional, you can safely remove them

    description = "High quality JAV"
    authors = listOf("Jace")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 1 // will be 3 if unspecified

    // List of video source types. Users are able to filter for extensions in a given category.
    // You can find a list of available types here:
    // https://recloudstream.github.io/cloudstream/html/app/com.lagradost.cloudstream3/-tv-type/index.html
    tvTypes = listOf("NSFW")

    iconUrl = "https://www.google.com/s2/favicons?domain=javfree.sh&sz=%size%"

    language = "en"
}

android {
    namespace = "com.example" // Reemplaza esto con tu namespace real

    compileSdkVersion(30)

    defaultConfig {
        minSdk = 21
        targetSdk = 30
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8" // Required
            // Disables some unnecessary features
            freeCompilerArgs = freeCompilerArgs +
                    "-Xno-call-assertions" +
                    "-Xno-param-assertions" +
                    "-Xno-receiver-assertions"
        }
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-Xskip-metadata-version-check")
    }
}
