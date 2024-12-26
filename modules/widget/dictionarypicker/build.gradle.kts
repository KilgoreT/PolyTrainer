plugins {
    id("lexeme.android.feature")
}

android {
    namespace = "me.apomazkin.dictionarypicker"
}

dependencies {
    
    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":modules:core:ui"))
    
    implementation(project(":modules:widget:iconDropDowned"))
    
    implementation(composeLibs.lifecycleViewmodelCompose)
    implementation(composeLibs.lifecycleRuntimeCompose)
    implementation(composeLibs.activityCompose)
}