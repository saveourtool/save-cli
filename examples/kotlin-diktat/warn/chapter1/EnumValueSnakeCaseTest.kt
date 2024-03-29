@file:Suppress("PACKAGE_NAME_INCORRECT_PREFIX")

package com.saveourtool.save.test.resources.test.paragraph1.naming.enum_

// ;warn:5:1: [MISSING_KDOC_TOP_LEVEL] all public and internal top-level classes and functions should have Kdoc: EnumValueSnakeCaseTest (cannot be auto-corrected){{.*}}
// ;warn:35: [WRONG_DECLARATIONS_ORDER] declarations of constants and enum members should be sorted alphabetically: enum entries order is incorrect{{.*}}
// ;warn:12:5: [ENUMS_SEPARATED] enum is incorrectly formatted: enums must end with semicolon{{.*}}
enum class EnumValueSnakeCaseTest {
    // ;warn:$line+1:5: [ENUM_VALUE] enum values should be{{ in }}selected UPPER_CASE snake/PascalCase format: paSC_SAl_l{{.*}}
    paSC_SAl_l,

    // ;warn:5: [ENUM_VALUE] enum values{{ should }}be in selected{{ UPPER_CASE }}snake/PascalCase format: PascAsl_f{{.*}}
    PascAsl_f
    // ;warn:$line-2:5: [ENUMS_SEPARATED] enum is incorrectly formatted: last enum entry must end with a comma{{.*}}

}
