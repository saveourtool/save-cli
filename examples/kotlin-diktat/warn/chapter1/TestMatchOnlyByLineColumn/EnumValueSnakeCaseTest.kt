package com.saveourtool.diktat.test.resources.test.paragraph1.naming.enum_

// ;warn:3:1
// ;warn:6:35
// ;warn:10:5: This message will be ignored anyway, don't matter what this line contain
enum class EnumValueSnakeCaseTest {
    // ;warn:$line+1:5
    paSC_SAl_l,

    // ;warn:5
    PascAsl_f
    // ;warn:$line-2:5

    // ;warn:1:9:
}
