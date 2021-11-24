package test.smoke.src.main.kotlin

fun readFile(foo: Foo) {
    var bar: Bar
}

class D {val x = 0
fun bar(): Bar {val qux = 42; return Bar(qux)}
}
// ;warn:0: [TEST] JUST_A_TEST
// IGNORE_ME
# I WILL DISSAPEAR AS WELL
