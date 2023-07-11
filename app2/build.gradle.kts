plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.addapktoasset.app2"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.example.addapktoasset.app2"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }
    signingConfigs {
        register("release") {
            keyAlias = "release"
            keyPassword = "123456"
            storePassword = "123456"
            storeFile = file("${rootProject.projectDir}/release.jks")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.3.2"
    }
    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
}


abstract class DisplayApksTask: DefaultTask() {

    @get:InputFiles
    abstract val apkFolder: DirectoryProperty

    @get:Internal
    abstract val builtArtifactsLoader: Property<com.android.build.api.variant.BuiltArtifactsLoader>

    private var builtArtifacts: com.android.build.api.variant.BuiltArtifacts? = null

    @get:Internal
    val apks: List<String>
        get() = builtArtifacts!!.elements.map { it.outputFile }

    @TaskAction
    fun taskAction() {
        val builtArtifacts: com.android.build.api.variant.BuiltArtifacts = builtArtifactsLoader.get().load(apkFolder.get())
            ?: throw RuntimeException("Cannot load APKs")
        this.builtArtifacts = builtArtifacts

        builtArtifacts.elements.forEach {
            println("Got an APK at ${it.outputFile}, exists: ${File(it.outputFile).exists()}")
        }

    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        project.tasks.named<DisplayApksTask>("releaseDisplayApks") {
            apkFolder.set(variant.artifacts.get(com.android.build.api.artifact.SingleArtifact.APK))
            builtArtifactsLoader.set(variant.artifacts.getBuiltArtifactsLoader())
        }
    }
}

val releaseDisplayApks = project.tasks.register<DisplayApksTask>("releaseDisplayApks") {
    doLast {
        copy {
            println("apks=$apks")
            from(*apks.toTypedArray())
            into("${buildDir}/apkFile")
        }
    }
}

configurations {
    create("apkFile") {
        isCanBeConsumed = true
        isCanBeResolved = false
    }
}

artifacts {
    add("apkFile", File("${buildDir}/apkFile")) {
        builtBy(releaseDisplayApks)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.8.0")
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.8.0"))
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.3.1")
    implementation("androidx.activity:activity-compose:1.5.1")
    implementation(platform("androidx.compose:compose-bom:2022.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2022.10.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
