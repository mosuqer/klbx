import nl.littlerobots.vcu.plugin.resolver.VersionSelectors

plugins {
    alias(libs.plugins.versionCatalogUpdate)
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloade
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.serialization) apply false
}

versionCatalogUpdate {
    versionSelector(VersionSelectors.STABLE)
}

