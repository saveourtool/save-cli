package com.saveourtool.save.IgnoreLinesTest.NoIgnoreLines

class D {
    val x = 0

    /**
     * @return
     */
    fun bar(): Bar {
        val qux = 42
        return Bar(qux)
    }
}

/**
 * @param foo
 */
fun readFile(foo: Foo) {
    var bar: Bar
}
// IGNORE_ME
