package com.saveourtool.diktat.test.resources.test.paragraph1.naming.enum_

// ;warn:3:1: [MISSING_KDOC_TOP_LEVEL] all public and internal top-level classes and functions should have Kdoc: EnumTestDetection (cannot be auto-corrected){{.*}}
// ;warn:30: [WRONG_DECLARATIONS_ORDER] declarations of constants and enum members should be sorted alphabetically: enum entries order is incorrect{{.*}}
// ;warn:10:5: [ENUMS_SEPARATED] enum is incorrectly formatted: enums must end with semicolon{{.*}}
enum class EnumTestDetection {
    // ;warn:$line+1:5: [ENUM_VALUE] enum values should be{{ in }}selected UPPER_CASE snake/PascalCase format: paSC_SAl_l{{.*}}
    paSC_SAl_l,

    // ;warn:5: [ENUM_VALUE] enum values{{ should }}be in selected{{ UPPER_CASE }}snake/PascalCase format: PascAsl_f{{.*}}
    PascAsl_f
    // ;warn:$line-2:5: [ENUMS_SEPARATED] enum is incorrectly formatted: last enum entry must end with a comma{{.*}}

    // ;warn:1:9: {{.*}}[PACKAGE_NAME_INCORRECT_PREFIX] package name should start from company's domain: com.saveourtool.save{{.*}}
}
// ;warn:0:0: [YOU SHOULD NOT SEE THIS] this warning should not be shown{{.*}}
