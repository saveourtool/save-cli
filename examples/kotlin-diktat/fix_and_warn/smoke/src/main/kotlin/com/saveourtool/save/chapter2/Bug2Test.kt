package test.smoke

import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.Properties

@ExperimentalStdlibApi public data class Example(val foo: Int, val bar: Double) : SuperExample("lorem ipsum")

private class Test : Exception()
/*    this class is unused */
// private class Test : RuntimeException()

internal fun createWithFile(
    runConfiguration: RunConfiguration,
    containerName: String,
    file: File,
    resources: Collection<File> = emptySet()
) {}

private fun foo(node: ASTNode) {
    when (node.elementType) {
        CLASS, FUN, PRIMARY_CONSTRUCTOR, SECONDARY_CONSTRUCTOR -> checkAnnotation(node)
    }
    val qwe = a &&
            b
    val qwe = a &&
            b

    if (x) // comment
        foo()

    setOf<Object>(IOException(), Properties(), LoggerFactory())
}