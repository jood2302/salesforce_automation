package service.testcaseverification;

import com.aquiva.autotests.rc.model.testcaseverification.TestCaseDTO;
import com.aquiva.autotests.rc.model.testcaseverification.TestCaseWithErrorDTO;
import io.github.classgraph.ClassGraph;
import io.qameta.allure.Step;
import io.qameta.allure.TmsLink;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.aquiva.autotests.rc.internal.reporting.ServiceTaskLogger.initializeAndGetResultsFile;
import static com.aquiva.autotests.rc.internal.reporting.ServiceTaskLogger.updateResultsFile;
import static java.util.stream.Collectors.toSet;

/**
 * Methods for verifying test cases from TestIt TMS and Automation Repository.
 */
public class TestCaseVerificationSteps {
    private static final String ANNOTATED_METHOD_NAME = "test";

    public final File resultsFile;
    private final List<TestCaseWithErrorDTO> testCasesWithErrors;

    public TestCaseVerificationSteps(String fileNamePrefix) throws IOException {
        resultsFile = initializeAndGetResultsFile(fileNamePrefix);
        testCasesWithErrors = new ArrayList<>();
    }

    /**
     * Find all of automated test cases IDs and return a collection of them.
     *
     * @return set of test cases IDs without duplicates
     */
    @Step("Get the collection of all automated test cases IDs in project")
    public Set<String> getTmsTestCasesIds() {
        var classGraph = new ClassGraph().enableAllInfo();

        try (var scanResult = classGraph.scan()) {
            return scanResult.getClassesWithMethodAnnotation(TmsLink.class)
                    .stream()
                    .flatMap(classInfo -> classInfo.getMethodInfo(ANNOTATED_METHOD_NAME)
                            .stream()
                            .flatMap(methodInfo -> methodInfo.getAnnotationInfoRepeatable(TmsLink.class)
                                    .stream()
                                    .flatMap(annotationInfo -> annotationInfo.getParameterValues().stream())
                                    .map(annotationParameterValue -> annotationParameterValue.getValue().toString())))
                    .collect(toSet());
        } catch (Exception e) {
            throw new RuntimeException("Something went wrong while parsing classes with annotated methods. " +
                    "Details: " + e.getMessage(), e);
        }
    }

    /**
     * Add data on the test case with an error to the results file.
     *
     * @param testCase     test case object that store information from TestIt
     * @param errorDetails text with test case's verification error
     *                     (e.g. "CRM-666 is in JUNK folder")
     * @throws IOException in case of an I/O error
     */
    public void updateResultsWithTestCaseWithError(TestCaseDTO testCase, String errorDetails) throws IOException {
        var testCaseWithError = new TestCaseWithErrorDTO(testCase.getTmsLinkId(), testCase.name, errorDetails);
        testCasesWithErrors.add(testCaseWithError);
        updateResultsFile(resultsFile, testCasesWithErrors);
    }
}
