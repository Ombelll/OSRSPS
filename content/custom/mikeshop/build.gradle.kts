plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.db)
    implementation(projects.api.parsers.jackson)
    implementation(projects.api.parsers.json)
    implementation(projects.api.script)
    implementation(projects.api.scriptAdvanced)
    implementation(projects.api.type.typeBuilders)
    implementation(projects.api.repo)
    implementation(projects.api.death)
    implementation(projects.content.interfaces.bank)
}
