package com.aquiva.autotests.rc.utilities;

import com.codeborne.selenide.Stopwatch;
import io.qameta.allure.Allure;

import java.time.Duration;

import static io.qameta.allure.Allure.step;
import static io.qameta.allure.model.Status.FAILED;
import static io.qameta.allure.model.Status.PASSED;
import static java.lang.System.nanoTime;
import static java.time.Duration.ofNanos;

/**
 * Utility class that allows to perform assertions with a given timeout.
 * <br/><br/>
 * Use class's methods for <i>long-running</i> API/DB related assertions.
 * <br/>
 * <b> Note: Only JUnit 5 {@link org.junit.jupiter.api.Assertions} methods work correctly
 * with Allure AND Beats reporting systems. AssertJ's method report unwanted FAILED steps
 * that result in a FAILED status for test in Beats!</b>
 * <br/>
 * <b> Note: for all Web UI-related assertions, refer to Selenide's API! </b>
 */
public class TimeoutAssertions {
    private static final Duration DEFAULT_POLLING_INTERVAL_MILLIS = Duration.ofMillis(1_000);

    /**
     * Perform the assertion with a given timeout.
     * <br/><br/>
     * Useful method for assertions that check data returned via SOAP/REST API calls
     * when the returned result might not be correct right after the call.
     *
     * @param runnableWithAssertion piece of code that contains the assertion to be made
     *                              (JUnit 5 {@link org.junit.jupiter.api.Assertions} methods).
     *                              <b> Note: make sure to place API call that returns the data
     *                              to be checked right here! </b>
     * @param timeout               max time interval to check the assertion for
     */
    public static void assertWithTimeout(Allure.ThrowableRunnableVoid runnableWithAssertion,
                                         Duration timeout) {
        assertWithTimeout(() -> {
            runnableWithAssertion.run();
            return null;
        }, timeout, DEFAULT_POLLING_INTERVAL_MILLIS);
    }

    /**
     * Perform the assertion with a given timeout.
     * <br/><br/>
     * Useful method for assertions that check data returned via SOAP/REST API calls
     * when the returned result might not be correct right after the call.
     *
     * @param runnableWithAssertion piece of code that contains the assertion to be made
     *                              (JUnit 5 {@link org.junit.jupiter.api.Assertions} methods).
     *                              <b> Note: make sure to place API call that returns the data
     *                              to be checked right here! </b>
     * @param timeout               max time interval to check the assertion for
     * @return any object that is obtained in this runnable operation
     * (e.g. Account object found via SOQL that can also be used outside this assertion).
     */
    public static <T> T assertWithTimeout(Allure.ThrowableRunnable<T> runnableWithAssertion,
                                          Duration timeout) {
        return assertWithTimeout(runnableWithAssertion, timeout, DEFAULT_POLLING_INTERVAL_MILLIS);
    }

    /**
     * Perform the assertion with a given timeout and polling interval.
     * <br/><br/>
     * Useful method for assertions that check data returned via SOAP/REST API calls
     * when the returned result might not be correct right after the call.
     *
     * @param runnableWithAssertion piece of code that contains the assertion to be made
     *                              (JUnit 5 {@link org.junit.jupiter.api.Assertions} methods).
     *                              <b> Note: make sure to place API call that returns the data
     *                              to be checked right here! </b>
     * @param timeout               max time interval to check the assertion for
     * @param pollingInterval       time interval for a delay between tries
     * @return any object that is obtained in this runnable operation
     * (e.g. Account object found via SOQL that can also be used outside this assertion).
     */
    public static <T> T assertWithTimeout(Allure.ThrowableRunnable<T> runnableWithAssertion,
                                          Duration timeout, Duration pollingInterval) {
        var stopwatch = new Stopwatch(timeout.toMillis());
        var startTimeNanos = nanoTime();
        Throwable lastError;
        do {
            try {
                var result = runnableWithAssertion.run();
                step("Check the given condition with a timeout", PASSED);
                return result;
            } catch (Throwable e) {
                lastError = e;
            }
            stopwatch.sleep(pollingInterval.toMillis());
        } while (!stopwatch.isTimeoutReached());

        var assertDuration = ofNanos(nanoTime()).minus(ofNanos(startTimeNanos));
        step(lastError.getMessage(), FAILED);
        throw new AssertionError(
                lastError.getMessage() +
                        "\nTimeout: " + assertDuration.toString(),
                lastError);
    }
}
