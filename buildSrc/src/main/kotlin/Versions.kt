@file:Suppress("CONSTANT_UPPERCASE")

object Versions {
    const val kotlin = "1.5.31"
    const val junit = "5.8.0"
    const val okio = "3.0.0-alpha.9"
    const val ktoml = "0.2.7"
    const val multiplatformDiff = "0.2.0"

    object Kotlinx {
        const val serialization = "1.2.2"
        const val datetime = "0.2.1"
        const val cli = "0.3.3"
        const val coroutines = "1.5.2-native-mt"
    }

    object IntegrationTest {
        const val ktlint = "0.39.0"
        const val ktlintLink = "https://github.com/pinterest/ktlint/releases/download/$ktlint/ktlint"
        const val diktat = "1.0.0-rc.2"
        const val diktatLink = "https://github.com/cqfn/diKTat/releases/download/v$diktat/diktat-$diktat.jar"
    }
}
