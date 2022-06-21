rootProject.name = "save"
includeBuild("build-logic")
include("save-common")
include("save-core")
include("save-cli")
include("save-plugins:fix-and-warn-plugin")
include("save-plugins:fix-plugin")
include("save-plugins:warn-plugin")
include("save-reporters")
include("save-common-test")

dependencyResolutionManagement {
    // todo: build-logic-settings
    repositories {
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/saveourtool/sarif4k")
            val gprUser: String? by settings
            val gprKey: String? by settings
            credentials {
                username = gprUser
                password = gprKey
            }
            content {
                includeGroup("io.github.detekt.sarif4k")
            }
        }
    }
}
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
