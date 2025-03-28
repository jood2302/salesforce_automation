package service.testcaseverification;

import base.BaseTest;
import com.aquiva.autotests.rc.model.testcaseverification.TestCaseDTO;
import io.qameta.allure.Description;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static com.aquiva.autotests.rc.internal.reporting.ServiceTaskLogger.logResults;
import static com.aquiva.autotests.rc.model.testcaseverification.TestCaseDTO.*;
import static com.aquiva.autotests.rc.model.testcaseverification.TestCaseDTO.Ascendant.JUNK_FOLDER_NAME;
import static com.aquiva.autotests.rc.utilities.testit.TestItRestApiClient.getTestCaseInfo;
import static io.qameta.allure.Allure.step;
import static java.util.Arrays.stream;

/**
 * Special task for verifying automated test cases in TestIt TMS.
 */
public class VerifyAutomatedCasesInTMS extends BaseTest {
    private final TestCaseVerificationSteps testCaseVerificationSteps;

    public VerifyAutomatedCasesInTMS() throws IOException {
        testCaseVerificationSteps = new TestCaseVerificationSteps("test_cases_to_check");
    }

    @Test
    @DisplayName("Verify automated test cases in TestIt")
    @Description("Verify that the automated test cases in the QA Automation Repository " +
            "exist in the TestIt TMS, have a correct priority, execution type, and are not located in the 'JUNK' folder")
    public void test() throws IOException {
        var testCasesIds = testCaseVerificationSteps.getTmsTestCasesIds();

        step("Get info about individual test cases from TestIt TMS and verify it", () -> {
            for (var testCaseId : testCasesIds) {
                step("Get '" + testCaseId + "' test case info and verify it", () -> {
                    var testCase = getTestCaseInfo(testCaseId);

                    verifyThatTestCaseExists(testCase);
                    verifyTestCasePriority(testCase);
                    verifyTestCaseExecutionType(testCase);
                    verifyThatTestCaseIsNotInJunkFolder(testCase);
                });
            }
        });

        logResults(testCaseVerificationSteps.resultsFile);
    }

    /**
     * Check that the provided test case exists in TestIt TMS. If not, the test case ID is added to the results file
     * of tests for correction.
     *
     * @param testCase test case object that store information from TestIt
     */
    private void verifyThatTestCaseExists(TestCaseDTO testCase) {
        var testCaseId = testCase.getTmsLinkId();
        step("Verify that '" + testCaseId + "' test case is exist in TestIt", () -> {
            if (testCase.message != null && !testCase.message.isEmpty()) {
                var error = testCaseId + " doesn't exist in TestIt. Details: " + testCase.message;
                testCaseVerificationSteps.updateResultsWithTestCaseWithError(testCase, error);
            }
        });
    }

    /**
     * Check that provided test case has 'P0' or 'P1' Priority, if not, then it is added to the result file with list
     * of tests for correction.
     *
     * @param testCase test case object that store information from TestIt
     */
    private void verifyTestCasePriority(TestCaseDTO testCase) {
        var testCaseId = testCase.getTmsLinkId();
        var expectedPriorities = List.of(P0_PRIORITY, P1_PRIORITY);
        step("Verify '" + testCaseId + "' test case's Priority", () -> {
            if (!expectedPriorities.contains(testCase.priority)) {
                var error = String.format("%s has wrong priority: %d (expected: %s)",
                        testCaseId, testCase.priority, expectedPriorities);
                testCaseVerificationSteps.updateResultsWithTestCaseWithError(testCase, error);
            }
        });
    }

    /**
     * Check that provided test case has 'Automated' Execution Type, if not, then it is added to the result file with
     * list of tests for correction.
     *
     * @param testCase test case object that store information from TestIt
     */
    private void verifyTestCaseExecutionType(TestCaseDTO testCase) {
        var testCaseId = testCase.getTmsLinkId();
        step("Verify '" + testCaseId + "' test case's Execution Type", () -> {
            if (testCase.executionType != AUTOMATED_EXECUTION_TYPE) {
                var error = String.format("%s has wrong execution type: %d (expected: %d)",
                        testCaseId, testCase.executionType, AUTOMATED_EXECUTION_TYPE);
                testCaseVerificationSteps.updateResultsWithTestCaseWithError(testCase, error);
            }
        });
    }

    /**
     * Check that provided test case is not stored in 'JUNK' folder, if not, then it is added to the result file with
     * list of tests for correction.
     *
     * @param testCase test case object that store information from TestIt
     */
    private void verifyThatTestCaseIsNotInJunkFolder(TestCaseDTO testCase) {
        var testCaseId = testCase.getTmsLinkId();
        step("Verify that '" + testCaseId + "' test case is not stored in 'JUNK' folder", () -> {
            if (stream(testCase.ascendants).anyMatch(folder -> folder.name.equals(JUNK_FOLDER_NAME))) {
                var error = testCaseId + " is in JUNK folder";
                testCaseVerificationSteps.updateResultsWithTestCaseWithError(testCase, error);
            }
        });
    }
}
