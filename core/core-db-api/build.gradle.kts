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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
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

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
