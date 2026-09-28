import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "me.apomazkin.core_db_api"
    compileSdk = libs.versions.compileSdk.get().toInt()

    testOptions {
        targetSdk = libs.versions.targetSdk.get().toInt()
    }

    lint {
        targetSdk = libs.versions.targetSdk.get().toInt()
    }
    
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvm.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvm.get())
    }
    
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvm.get()))
    }
}

dependencies {

    // Coroutines
    implementation(kotlinLibs.coroutinesCore)
    implementation(datastoreLibs.paging)

    // IS481 (MIN-2): API DTO `ComponentValueApiEntity.data: ComponentValueData`,
    // `ComponentTypeApiEntity.systemKey: BuiltInComponent?` и т.д. — типы из domain.
    // `api` (а не `implementation`) — типы domain видны транзитивно через
    // core-db-api callsite'ам (core-db-impl, app, screen modules).
    api(project(":modules:domain:lexeme"))
    // IS493 Э3: outcomes групп (CreateGroupOutcome и пр.) — в сигнатурах
    // GroupApi; тот же приём, что и для lexeme (типы видны транзитивно).
    api(project(":modules:domain:group"))

    testImplementation(testLibs.junit)
    androidTestImplementation(testLibs.androidxTestExt)
    androidTestImplementation(testLibs.espressoCore)
}
