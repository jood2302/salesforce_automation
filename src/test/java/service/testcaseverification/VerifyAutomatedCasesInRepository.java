package service.testcaseverification;

import base.BaseTest;
import io.qameta.allure.Description;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.aquiva.autotests.rc.internal.reporting.ServiceTaskLogger.logResults;
import static com.aquiva.autotests.rc.utilities.testit.TestItRestApiClient.getAutomatedTmsTestCasesIds;
import static com.aquiva.autotests.rc.utilities.testit.TestItRestApiClient.getTestCaseInfo;
import static io.qameta.allure.Allure.step;

/**
 * Special task for verifying automated test cases in TestIt TMS
 * against the automated test cases in QA Automation Repository.
 */
public class VerifyAutomatedCasesInRepository extends BaseTest {
    private final TestCaseVerificationSteps testCaseVerificationSteps;

    public VerifyAutomatedCasesInRepository() throws IOException {
        testCaseVerificationSteps = new TestCaseVerificationSteps("non_existing_test_cases");
    }

    @Test
    @DisplayName("Verify 'Automated' or 'Ready for Automation' tests in TestIt TMS")
    @Description("Verify if the test cases with Execution Type = 'Automated' or 'Ready for Automation' in TestIt TMS " +
            "are present in the Automation Repository")
    public void test() throws IOException {
        var testCasesIdsInRepository = testCaseVerificationSteps.getTmsTestCasesIds();
        var testCasesIdsInTMS = getAutomatedTmsTestCasesIds();

        step("Get the info about individual 'Automated'/'Ready for Automation' test cases in CRM project in TestIt TMS, " +
                "and verify if they are present in the QA Automation Repository", () -> {
            for (var automatedTestCaseIdFromTMS : testCasesIdsInTMS) {
                step("Verify '" + automatedTestCaseIdFromTMS + "' test case", () -> {
                    if (!testCasesIdsInRepository.contains(automatedTestCaseIdFromTMS)) {
                        var testCaseInfo = getTestCaseInfo(automatedTestCaseIdFromTMS);
                        var error = automatedTestCaseIdFromTMS + " is not found in the Automation Repository. " +
                                "Execution Type: " + testCaseInfo.executionType;
                        testCaseVerificationSteps.updateResultsWithTestCaseWithError(testCaseInfo, error);
                    }
                });
            }
        });

        logResults(testCaseVerificationSteps.resultsFile);
    }
}
