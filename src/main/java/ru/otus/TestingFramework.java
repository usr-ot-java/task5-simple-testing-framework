package ru.otus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.annotations.After;
import ru.otus.annotations.Before;
import ru.otus.annotations.Test;
import ru.otus.exception.InitializationTestException;
import ru.otus.statistics.TestCase;
import ru.otus.statistics.TestStatistics;
import ru.otus.statistics.TestStatisticsHolder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TestingFramework {

    private static final Logger logger = LoggerFactory.getLogger(TestingFramework.class);

    private static final String TEMPLATE_MULTIPLE_ANNOTATIONS_EXCEPTION =
            "Method %s in class %s cannot have @%s and @%s annotations at the same time";
    private static final String TEMPLATE_CLASS_CONSTRUCTOR_EXCEPTION =
            "Class %s must have one public no-arg constructor";

    private static final Set<Class<?>> TESTING_ANNOTATIONS = Set.of(After.class, Before.class, Test.class);

    /*
        * Main method to run the testing.
        * @param cls - Test class
     */
    public static void runTest(Class<?> cls) {
        validateTestingClass(cls);

        Method[] methods = cls.getMethods();

        List<Method> beforeMethods = new ArrayList<>();
        List<Method> afterMethods = new ArrayList<>();
        List<Method> testMethods = new ArrayList<>();

        for (Method method : methods) {
            Class<?> annTypeClass = validateAndGetAnnotationTypeClass(cls, method);

            if (annTypeClass == null) {
                continue;
            }

            validateTestingMethodDeclaration(cls, method);

            if (annTypeClass.equals(After.class)) {
                afterMethods.add(method);
            } else if (annTypeClass.equals(Before.class)) {
                beforeMethods.add(method);
            } else if (annTypeClass.equals(Test.class)) {
                testMethods.add(method);
            }
        }

        TestStatistics testStatistics = runTests(cls, beforeMethods, afterMethods, testMethods);
        printTestStatistics(testStatistics);
    }



    private static void validateTestingClass(Class<?> cls) {
        // TODO: remove validation? Since cls.getMethods() returns only public methods
        if (!Modifier.isPublic(cls.getModifiers())) {
            throw new InitializationTestException(
                    String.format("Class %s must have public access modifier", cls.getName())
            );
        }

        Constructor[] constructors = cls.getConstructors();
        if (constructors.length != 1) {
            throw new InitializationTestException(
                    String.format(TEMPLATE_CLASS_CONSTRUCTOR_EXCEPTION, cls.getName())
            );
        }
        Constructor constructor = constructors[0];
        if (constructor.getParameterCount() != 0 || !Modifier.isPublic(constructor.getModifiers())) {
            throw new InitializationTestException(
                    String.format(TEMPLATE_CLASS_CONSTRUCTOR_EXCEPTION, cls.getName())
            );
        }
    }

    private static void validateTestingMethodDeclaration(Class<?> cls, Method method) {
        if (!Modifier.isPublic(method.getModifiers())) {
            throw new InitializationTestException(
                    String.format("Method %s in class %s must have public access modifier", method.getName(), cls.getName())
            );
        }

        if (method.getParameterCount() > 0) {
            throw new InitializationTestException(
                    String.format("Method %s in class %s is not allowed to have arguments", method.getName(), cls.getName())
            );
        }
    }

    private static Class<?> validateAndGetAnnotationTypeClass(Class<?> cls, Method method) {
        Annotation[] annotations = method.getAnnotations();

        Class<?> result = null;
        for (Annotation annotation : annotations) {
            if (TESTING_ANNOTATIONS.contains(annotation.annotationType())) {
                if (result != null) {
                    throw new InitializationTestException(
                            String.format(TEMPLATE_MULTIPLE_ANNOTATIONS_EXCEPTION, method.getName(), cls.getName(), annotation.annotationType().getName(), result.getName())
                    );
                }
                result = annotation.annotationType();
            }
        }

        return result;
    }

    private static TestStatistics runTests(Class<?> cls,
                                           List<Method> beforeMethods,
                                           List<Method> afterMethods,
                                           List<Method> testMethods) {

        TestStatisticsHolder testStatisticsCalculator = new TestStatisticsHolder();

        for (Method testMethod : testMethods) {
            Object classInstance = createTestClassInstance(cls);
            beforeMethods.forEach(method -> runBeforeMethod(cls, classInstance, method));
            long startTime = System.currentTimeMillis();
            boolean result = runTestMethod(cls, classInstance, testMethod);
            long testExecutionTime = System.currentTimeMillis() - startTime;
            TestCase testCase = new TestCase(testMethod.getName(), result, testExecutionTime);
            testStatisticsCalculator.addTestCase(testCase);
            afterMethods.forEach(method -> runAfterMethod(cls, classInstance, method));
        }

        return new TestStatistics(testStatisticsCalculator.getSuccesses(), testStatisticsCalculator.getFailures());
    }

    private static Object createTestClassInstance(Class<?> cls) {
        try {
            return cls.getDeclaredConstructors()[0].newInstance();
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new InitializationTestException(
                    String.format("Failed to create instance of the class %s", cls)
            );
        }
    }

    private static void runBeforeMethod(Class<?> cls, Object testClassInstance, Method method) {
        try {
            method.invoke(testClassInstance);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new InitializationTestException(
                    String.format("Failed to execute before method %s in class %s", method.getName(), cls.getName())
            );
        }
    }

    private static boolean runTestMethod(Class<?> cls, Object testClassInstance, Method method) {
        try {
            method.invoke(testClassInstance);
            return true;
        } catch (InvocationTargetException | IllegalAccessException e) {
            logger.error(
                    String.format("Exception occurred during execution of test method %s in class %s: %s", method.getName(), cls.getName(), e.getMessage())
            );
        }

        return false;
    }

    private static void runAfterMethod(Class<?> cls, Object testClassInstance, Method method) {
        try {
            method.invoke(testClassInstance);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new InitializationTestException(
                    String.format("Failed to execute after method %s in class %s", method.getName(), cls.getName())
            );
        }
    }

    private static void printTestStatistics(TestStatistics testStatistics) {
        int total = testStatistics.getSuccessesTotal() + testStatistics.getFailuresTotal();
        System.out.println();
        System.out.println(String.format("Tests run: %d", total));
        System.out.println(String.format("Succeeded: %d", testStatistics.getSuccessesTotal()));
        System.out.println(String.format("Failed: %d", testStatistics.getFailuresTotal()));

        System.out.println();
        System.out.println("Extended statistics for the succeeded tests");
        testStatistics.getSuccesses().forEach(testCase -> System.out.println(String.format("Method %s. Time taken: %d ms", testCase.getName(), testCase.getExecutionTime())));

        System.out.println();
        System.out.println("Extended statistics for the failed tests");
        testStatistics.getFailures().forEach(testCase -> System.out.println(String.format("Method %s. Time taken: %d ms", testCase.getName(), testCase.getExecutionTime())));
    }

}
