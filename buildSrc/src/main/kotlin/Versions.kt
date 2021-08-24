@file:Suppress("CONSTANT_UPPERCASE")

object Versions {
    const val kotlin = "1.5.21"
    const val junit = "5.7.2"
    const val okio = "3.0.0-alpha.8"
    const val ktoml = "0.2.7"
    const val multiplatformDiff = "0.2.0"
    const val ktorVersion = "1.6.1"

    object Kotlinx {
        const val serialization = "1.2.2"
        const val datetime = "0.2.1"
        const val cli = "0.3.2"
    }

    object IntegrationTest {
        const val ktlint = "0.39.0"
        const val ktlintLink = "https://github.com/pinterest/ktlint/releases/download/$ktlint/ktlint"
        const val diktat = "1.0.0-rc.2"
        const val diktatLink = "https://github.com/cqfn/diKTat/releases/download/v$diktat/diktat-$diktat.jar"
    }
}
