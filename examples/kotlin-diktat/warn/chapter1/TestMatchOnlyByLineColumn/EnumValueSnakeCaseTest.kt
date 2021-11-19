package org.cqfn.diktat.test.resources.test.paragraph1.naming.enum_

// ;warn:3:1: a
// ;warn:35: a
// ;warn:10:5: This message will be ignored anyway, don't matter what this line contain+
enum class EnumValueSnakeCaseTest {
    // ;warn:$line+1:5: s
    paSC_SAl_l,

    // ;warn:5: a
    PascAsl_f
    // ;warn:$line-2:5: a+

    // ;warn:1:9: a+
}
