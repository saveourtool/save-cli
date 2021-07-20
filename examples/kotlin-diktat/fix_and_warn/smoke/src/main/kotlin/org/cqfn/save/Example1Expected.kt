// ;warn:1:1 [FILE_NAME_MATCH_CLASS] file name is incorrect - it should match with the class described in it if there is the only one class declared: Example1Expected.kt vs Example
package org.cqfn.save

// ;warn:4:1 [MISSING_KDOC_TOP_LEVEL] all public and internal top-level classes and functions should have Kdoc: Example (cannot be auto-corrected)
// ;warn:4:1 [USE_DATA_CLASS] this class can be converted to a data class: Example (cannot be auto-corrected)
class Example {
    // ;warn:7:5 [MISSING_KDOC_CLASS_ELEMENTS] all public, internal and protected classes, functions and variables inside the class should have Kdoc: isValid (cannot be auto-corrected)
    @get:JvmName("getIsValid")
    val isValid = true
}
