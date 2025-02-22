plugins {
    id("lexeme.android.feature")
}

android {
    namespace = "me.apomazkin.quiz.chat"
}

dependencies {
    
    implementation(project("path" to ":modules:core:mate"))
    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":modules:core:ui"))
    implementation(project("path" to ":core:core-resources"))
    implementation(project("path" to ":modules:widget:dictionarypicker"))
    
    implementation(composeLibs.lifecycleViewmodelCompose)
    implementation(composeLibs.lifecycleRuntimeCompose)
    implementation(composeLibs.activityCompose)
    
}