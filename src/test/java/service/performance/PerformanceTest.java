package service.performance;

import java.lang.annotation.*;

/**
 * The PerformanceTest annotation is used to mark tests that are intended to measure performance.
 * This annotation allows you to highlight tests that should be run in a special performance mode
 * and collect performance metrics for analysis.
 * The test will run with performance measurement if -DperformanceTesting = true.
 * By default, this parameter is false.
 *
 * @see PerformanceMeasureAspect
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PerformanceTest {
}
