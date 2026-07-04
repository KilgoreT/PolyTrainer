plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "me.apomazkin.core_db_api"
    compileSdk = 35
    
    defaultConfig {
        minSdk = 23
        targetSdk = 35
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
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

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
