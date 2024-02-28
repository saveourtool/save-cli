package com.saveourtool.save.core.runners

import com.saveourtool.save.core.annotations.CheckedLanguage
import com.saveourtool.save.core.annotations.AnnotationForClass
import com.saveourtool.save.core.annotations.AnnotationForMethods
import org.junit.AssumptionViolatedException
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunNotifier
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runners.model.FrameworkMethod
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method


/**
 * Runner, which provide interface for running tests, by setting configuration via annotations
 */
class CommonTestRunner(private val testClass: Class<*>) : BlockJUnit4ClassRunner(testClass) {
    /**
     * Run test cases in [testClass], annotated with [AnnotationForClass]
     *
     *
     * If individual test is annotated with [AnnotationForMethods], run doTest()
     * otherwise, invoke test as is
     *
     * @param frameworkMethod running method
     * @param notifier notifier, which fires about actual status of test
     */
    override fun runChild(
        frameworkMethod: FrameworkMethod,
        notifier: RunNotifier
    ) {
        val method = frameworkMethod.method
        val methodDescription = Description.createTestDescription(
            testClass, method.name
        )
        notifier.fireTestStarted(methodDescription)
        try {
            if (testClass.isAnnotationPresent(Ignore::class.java) ||
                method.isAnnotationPresent(Ignore::class.java)
            ) {
                notifier.fireTestIgnored(methodDescription)
                return
            }
            val testObject = testClass.getDeclaredConstructor().newInstance()
            doRunChild(testObject, method, methodDescription, notifier)
            notifier.fireTestFinished(methodDescription)
        } catch (e: AssumptionViolatedException) {
            val failure = Failure(methodDescription, e)
            notifier.fireTestAssumptionFailed(failure)
        } catch (e: Exception) {
            val failure = Failure(methodDescription, e)
            notifier.fireTestFailure(failure)
        }
    }

    private fun doRunChild(
        testObject: Any,
        method: Method,
        methodDescription: Description,
        notifier: RunNotifier
    ) {
        try {
            if (testClass.isAnnotationPresent(Ignore::class.java) || method.isAnnotationPresent(Ignore::class.java)) {
                notifier.fireTestIgnored(methodDescription)
                return
            }
            if (testClass.isAnnotationPresent(AnnotationForClass::class.java) &&
                method.isAnnotationPresent(Test::class.java) &&
                method.isAnnotationPresent(AnnotationForMethods::class.java)
            ) {
                // TODO:
                // here you can collect configuration from annotations and do test
                // doTest()

                // if needed, invoke test itself
                method.invoke(testObject)
            } else if (testClass.isAnnotationPresent(AnnotationForClass::class.java) &&
                method.isAnnotationPresent(Test::class.java)
            ) {
                // support the case, when test is not annotated for some reasons,
                // simply run the test
                method.invoke(testObject)
            }
        } catch (e: InvocationTargetException) {
            val targetException = e.targetException
            // we need to distinguish AssumptionViolatedException separately
            if (targetException is AssumptionViolatedException) {
                throw targetException
            } else {
                throw e
            }
        }
    }

    // ==================== TestConfigParams Getters ====================== //

    private fun databaseSuffix(): String {
        return testClass.getAnnotation(AnnotationForClass::class.java).databaseSuffix
    }

    private fun ruleSuffix(): String {
        return testClass.getAnnotation(AnnotationForClass::class.java).ruleSuffix
    }

    private fun codeBasePath(): String {
        return testClass.getAnnotation(AnnotationForClass::class.java).codeBasePath
    }

    private fun databasePath(): String {
        return testClass.getAnnotation(AnnotationForClass::class.java).databasePath
    }

    private fun ruleBasePath(): String {
        return testClass.getAnnotation(AnnotationForClass::class.java).ruleBasePath
    }

    private fun checkedLanguage(): CheckedLanguage {
        return testClass.getAnnotation(AnnotationForClass::class.java).checkedLanguage
    }

    private fun needParseSourceFile(): Boolean {
        return testClass.getAnnotation(AnnotationForClass::class.java).needParseSourceFile
    }

    // ==================== TestcaseConfigParams Getters ====================== //

    private fun sourcePackage(method: Method): String {
        return method.getAnnotation(AnnotationForMethods::class.java).sourcePackage
    }

    private fun headerPackage(method: Method): String {
        return method.getAnnotation(AnnotationForMethods::class.java).headerPackage
    }

    private fun jarPackage(method: Method): String {
        return method.getAnnotation(AnnotationForMethods::class.java).jarPackage
    }

    private fun databaseName(method: Method): String {
        return method.getAnnotation(AnnotationForMethods::class.java).databaseName
    }

    private fun ruleName(method: Method): String {
        return method.getAnnotation(AnnotationForMethods::class.java).ruleName
    }

    private fun resultSize(method: Method): Int {
        return method.getAnnotation(AnnotationForMethods::class.java).resultSize
    }

    private fun valid(method: Method): Boolean {
        return method.getAnnotation(AnnotationForMethods::class.java).valid
    }
}

