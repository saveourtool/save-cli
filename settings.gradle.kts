rootProject.name = "save"
include("save-common")
include("save-core")
include("save-cli")
include("save-plugins:fix-and-warn-plugin")
include("save-plugins:fix-plugin")
include("save-plugins:warn-plugin")
include("save-reporters")
include("save-common-test")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
