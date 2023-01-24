rootProject.name = "save"
include("save-common")
include("save-core")
include("save-cli")
include("save-plugins:fix-and-warn-plugin")
include("save-plugins:fix-plugin")
include("save-plugins:warn-plugin")
include("save-reporters")
include("save-common-test")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        // add sonatype repository
        maven {
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        }
    }
}
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
