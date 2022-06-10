// ;warn:2:9: [PACKAGE_NAME_INCORRECT_PREFIX] package name should start from company's domain: com.saveourtool.save{{.*}}
package com.saveourtool.diktat.test.resources.test.paragraph1.naming.enum_

// ;warn:4:1: [MISSING_KDOC_TOP_LEVEL] all public and internal top-level classes and functions should have Kdoc: EnumValueSnakeCaseTest (cannot be auto-corrected){{.*}}
// ;warn:8:35: [WRONG_DECLARATIONS_ORDER] declarations of constants and enum members should be sorted alphabetically: enum entries order is incorrect{{.*}}
// ;warn:17:5: [ENUMS_SEPARATED] enum is incorrectly formatted: enums must end with semicolon{{.*}}
// ;warn:17:5: [ENUMS_SEPARATED] enum is incorrectly formatted: last enum entry must end with a comma{{.*}}
enum class EnumValueSnakeCaseTest {
    // ;warn:10:5: [ENUM_VALUE] enum values should be in selected UPPER_CASE snake/PascalCase format: paSC_SAl_l{{.*}}
    paSC_SAl_l,
    // ;warn:12:5: [ENUM_VALUE] enum values should be in selected UPPER_CASE snake/PascalCase format: PascAsl_f{{.*}}
    PascAsl_f,
    // ;warn:14:5: [ENUM_VALUE] enum values should be in selected UPPER_CASE snake/PascalCase format: START_PSaaa_DFE{{.*}}
    START_PSaaa_DFE,
    // ;warn:16:5: [ENUM_VALUE] enum values should be in selected UPPER_CASE snake/PascalCase format: _NAme_MYa_sayR{{.*}}
    _NAme_MYa_sayR,
    // ;warn:18:5: [ENUM_VALUE] enum values should be in selected UPPER_CASE snake/PascalCase format: NAme_MYa_sayR_{{.*}}
    NAme_MYa_sayR_
}
