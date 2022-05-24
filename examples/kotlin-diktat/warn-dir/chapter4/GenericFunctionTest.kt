/* ;warn:1:1: [FILE_NAME_MATCH_CLASS] file name is incorrect - it should match with the class described in it if there is the only one class declared: GenericFunctionTest.kt vs ClassName */
package com.saveourtool.save.test.paragraph1.naming.generic

private class ClassName<T> {
    private fun <Template, T> lock(body: ((Template?) -> T?)?, value: Template?): T? {
        try {
            val variableName: Template
            /* ;warn:17: [VARIABLE_NAME_INCORRECT_FORMAT] variable name should
             * be in lowerCamelCase and should contain only latin (ASCII)
             * letters or numbers and should start from lower letter: variableT
             * */
            val variableT: T
            println(variableT)
            return body!!(variableName)
        } finally {
            println()
        }
    }

    /* ;warn:20:5: [MISSING_KDOC_ON_FUNCTION] all public, internal and protected
     * functions should have Kdoc with proper tags: foo */
    fun foo(var1: T, var2: ((T?) -> T?)?) {
        lock<T, T>(var2, var1)
    }
}
