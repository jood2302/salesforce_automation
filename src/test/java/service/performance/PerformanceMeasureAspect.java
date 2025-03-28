package service.performance;

import org.apache.commons.io.FileUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.aquiva.autotests.rc.internal.reporting.ServiceTaskLogger.logResults;
import static java.lang.Boolean.parseBoolean;
import static java.lang.System.currentTimeMillis;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.util.Arrays.stream;

/**
 * Aspect class for performance measuring of particular methods of the so-called "performance tests".
 * <br/><br/>
 * Right now, this class helps to create performance measuring report in Allure Reports.
 *
 * @see PerformanceTest
 */
@SuppressWarnings("unused")
@Aspect
public class PerformanceMeasureAspect {
    private static final boolean IS_PERFORMANCE_TESTING_ON = parseBoolean(System.getProperty("performanceTesting", "false"));

    /**
     * Resulting mapping for operations' time metrics:
     * operation name to operation's execution time (in milliseconds).
     * <br/>
     * Example:
     * <pre><code>
     * {
     *   "add_products_loading": 10942,
     *   "create_new_quote": 33357,
     *   "save_quote_tab": 22029,
     *   "opp_to_landing": 39416,
     *   "save_price_tab": 53806,
     *   "sync_ngbs_loading": 29276,
     *   "save_package_tab": 43600
     * }
     * </code></pre>
     */
    private final ThreadLocal<Map<String, Long>> threadLocalResultMap = ThreadLocal.withInitial(HashMap::new);

    /**
     * Pointcut definition for executing QuoteWizardSteps.openQuoteWizardOnOpportunityRecordPage(..).
     */
    @Pointcut("call(* *..QuoteWizardSteps.openQuoteWizardOnOpportunityRecordPage(..)) " +
            "&& cflow(execution(* *..*.test()) && @within(service.performance.PerformanceTest))")
    public void opportunityQuoteWizardLandingPointcut() {
    }

    /**
     * Pointcut definition for executing QuoteWizardSteps.addNewSalesQuote().
     */
    @Pointcut("call(* *..QuoteWizardSteps.addNewSalesQuote()) " +
            "&& cflow(execution(* *..*.test()) && @within(service.performance.PerformanceTest))")
    public void addNewSalesQuotePointcut() {
    }

    /**
     * Pointcut definition for executing PackagePage.saveChanges().
     */
    @Pointcut("call(* *..PackagePage.saveChanges()) " +
            "&& cflow(execution(* *..*.test()) && @within(service.performance.PerformanceTest))")
    public void packagePageSaveChangesPointcut() {
    }

    /**
     * Pointcut definition for executing ProductsPage.openTab().
     */
    @Pointcut("call(* *..ProductsPage.openTab()) " +
            "&& cflow(execution(* *..*.test()) && @within(service.performance.PerformanceTest))")
    public void productsPageOpenTabPointcut() {
    }

    /**
     * Pointcut definition for executing CartPage.saveChanges().
     */
    @Pointcut("call(* *..CartPage.saveChanges()) " +
            "&& cflow(execution(* *..*.test()) && @within(service.performance.PerformanceTest))")
    public void cartPageSaveChangesPointcut() {
    }

    /**
     * Pointcut definition for executing QuotePage.saveChanges().
     */
    @Pointcut("call(* *..QuotePage.saveChanges()) " +
            "&& cflow(execution(* *..*.test()) && @within(service.performance.PerformanceTest))")
    public void quotePageSaveChangesPointcut() {
    }

    /**
     * Pointcut definition for executing SyncWithNgbsSteps.stepStartSyncWithNgbsViaProcessOrder(..).
     */
    @Pointcut("call(* *..SyncWithNgbsSteps.stepStartSyncWithNgbsViaProcessOrder(..)) " +
            "&& @within(service.performance.PerformanceTest)")
    public void stepStartSyncWithNgbsViaProcessOrderPointcut() {
    }

    /**
     * Pointcut definition for executing openProcessOrderModalForSignUp(..).
     */
    @Pointcut("call(* *..openProcessOrderModalForSignUp(..)) " +
            "&& @within(service.performance.PerformanceTest)")
    public void stepOpenProcessOrderModalForSignUpPointcut() {
    }

    /**
     * Measure and collect execution time in the context of methods in the performance test.
     *
     * @param joinPoint current intercepted context for aspect
     * @return the modified result.
     * @throws Throwable if an exception occurs during the execution.
     */
    @Around("opportunityQuoteWizardLandingPointcut() || addNewSalesQuotePointcut() || " +
            "packagePageSaveChangesPointcut() || productsPageOpenTabPointcut() || " +
            "cartPageSaveChangesPointcut() || quotePageSaveChangesPointcut() || " +
            "stepStartSyncWithNgbsViaProcessOrderPointcut() || " +
            "stepOpenProcessOrderModalForSignUpPointcut()")
    public Object measureTime(final ProceedingJoinPoint joinPoint) throws Throwable {
        if (IS_PERFORMANCE_TESTING_ON) {
            var startTime = currentTimeMillis();
            var result = joinPoint.proceed();
            var endTime = currentTimeMillis();
            var resultTime = endTime - startTime;
            var reportOperation = stream(Operations.values())
                    .filter(operation ->
                            joinPoint.getSignature().toShortString().equals(operation.getOperationName()) ||
                                    joinPoint.getSignature().getName().equals(operation.getOperationName()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("There's no advised operation! " +
                            "Re-check PerformanceMeasureAspect.Operations enum " +
                            "to make sure that the interrupted operation is there!"));
            threadLocalResultMap.get().put(reportOperation.getJsonField(), resultTime);
            return result;
        }
        return joinPoint.proceed();
    }

    /**
     * Generate json report and add it as attachment for the running test.
     */
    @Around("execution(* *..*.test()) && @within(service.performance.PerformanceTest)")
    public Object generateReport(ProceedingJoinPoint joinPoint) throws Throwable {
        if (IS_PERFORMANCE_TESTING_ON) {
            try {
                return joinPoint.proceed();
            } catch (Throwable e) {
                stream(Operations.values()).forEach(reportOperation ->
                        threadLocalResultMap.get().putIfAbsent(reportOperation.getJsonField(), -1L));
                throw new AssertionError(e);
            } finally {
                var fileName = String.format("performance_results_%s_%s.json",
                        joinPoint.getSignature().getDeclaringType().getSimpleName(),
                        LocalDateTime.now().format(ISO_LOCAL_DATE_TIME));
                var file = new File(fileName);
                var data = new JSONObject(threadLocalResultMap.get()).toString();
                FileUtils.write(file, data, StandardCharsets.UTF_8);
                logResults(file);

                threadLocalResultMap.remove();
            }
        }
        return joinPoint.proceed();
    }

    /**
     * Performance measurement operations.
     * To add a new operation for performance tracking, add a new operation to enum by adding
     * an operation in the form of Class.method() example ('ProductsPage.openTab()') and a key for the json report.
     */
    private enum Operations {
        OPPORTUNITY_LANDING("openQuoteWizardOnOpportunityRecordPage", "opp_to_landing"),
        CREATE_QUOTE("addNewSalesQuote", "create_new_quote"),
        SAVE_PACKAGE("PackagePage.saveChanges()", "save_package_tab"),
        ADD_PRODUCTS_LANDING("ProductsPage.openTab()", "add_products_loading"),
        SAVE_PRICE("CartPage.saveChanges()", "save_price_tab"),
        SAVE_QUOTE("QuotePage.saveChanges()", "save_quote_tab"),
        SYNC_NGBS("stepStartSyncWithNgbsViaProcessOrder", "sync_ngbs_loading"),
        SIGN_UP("openProcessOrderModalForSignUp", "sign_up_loading");

        private final String operationName;
        private final String jsonField;

        Operations(String operationName, String jsonField) {
            this.operationName = operationName;
            this.jsonField = jsonField;
        }

        /**
         * Get the performance measurement operation's name
         * (e.g. "openQuoteWizardOnOpportunityRecordPage").
         *
         * @return {@link String} performance measurement operation
         */
        public String getOperationName() {
            return operationName;
        }

        /**
         * Get the performance measurement operation's json key for the report
         * (e.g. "opp_to_landing").
         *
         * @return {@link String} String json key
         */
        public String getJsonField() {
            return jsonField;
        }
    }
}
