// ;warn:1:9 [PACKAGE_NAME_INCORRECT_PREFIX] package name should start from company's domain: org.cqfn.save
// ;warn:1:1 [FILE_NAME_MATCH_CLASS] file name is incorrect - it should match with the class described in it if there is the only one class declared: GenericFunctionTest.kt vs ClassName
package org.cqfn.diktat.test.paragraph1.naming.generic

private class ClassName<T> {
    private fun <Template, T> lock(body: ((Template?) -> T?)?, value: Template?): T? {
        try {
            // ;warn:6:13 [LOCAL_VARIABLE_EARLY_DECLARATION] local variables should be declared close to the line where they are first used: <variableName> is declared on line <6> and is used for the first time on line <9> (cannot be auto-corrected)
            // ;warn:6:43 [NULLABLE_PROPERTY_TYPE] try to avoid use of nullable types: initialize explicitly (cannot be auto-corrected)
            val variableName: Template? = null
            // ;warn:7:17 [VARIABLE_NAME_INCORRECT_FORMAT] variable name should be in lowerCamelCase and should contain only latin (ASCII) letters or numbers and should start from lower letter: variableT
            // ;warn:7:33 [NULLABLE_PROPERTY_TYPE] try to avoid use of nullable types: initialize explicitly (cannot be auto-corrected)
            val variableT: T? = null
            println(variableT)
            return body!!(variableName)
        } finally {
            println()
        }
    }

    // ;warn:15:5 [MISSING_KDOC_ON_FUNCTION] all public, internal and protected functions should have Kdoc with proper tags: foo
    fun foo(var1: T, var2: ((T?) -> T?)?) {
        lock<T, T>(var2, var1)
    }
}
