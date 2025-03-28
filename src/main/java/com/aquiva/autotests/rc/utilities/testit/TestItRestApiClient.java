package com.aquiva.autotests.rc.utilities.testit;

import com.aquiva.autotests.rc.model.testcaseverification.TestCaseDTO;
import com.aquiva.autotests.rc.utilities.RestApiClient;
import io.qameta.allure.Step;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Set;

import static com.aquiva.autotests.rc.utilities.RestApiAuthentication.usingNoAuthentication;
import static com.aquiva.autotests.rc.utilities.testit.TestItRestApiHelper.*;
import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;

/**
 * Class for handling calls to TestIt API.
 * <br/>
 * Useful for getting the data from TestIt for entities like test cases, etc...
 */
public class TestItRestApiClient {
    private static final RestApiClient CLIENT = new RestApiClient(
            usingNoAuthentication(),
            "Unable to get a response from TestIt! Details: "
    );

    /**
     * Get all the test case information.
     *
     * @return test case object with stored information
     */
    @Step("Get test case information via TestIt API")
    public static TestCaseDTO getTestCaseInfo(String testCaseId) {
        var url = getTestCaseInfoURL(testCaseId);

        return CLIENT.get(url, TestCaseDTO.class);
    }


    /**
     * Get list of test cases with Execution Type = 'Automated' or 'Ready for Automation' in TestIt TMS.
     *
     * @return set of test cases IDs with Execution Type = 'Automated' or 'Ready for Automation'
     */
    @Step("Get test cases with Execution Type = 'Automated' or 'Ready for Automation' via TestIt API")
    public static Set<String> getAutomatedTmsTestCasesIds() {
        var url = getTestCaseListURL();

        var response = CLIENT.post(url, SEARCH_AUTOMATED_TEST_CASES_QUERY);
        var testCaseArray = new JSONArray(response);

        return stream(testCaseArray.spliterator(), false)
                .map(testCase -> ((JSONObject) testCase).getInt("externalId"))
                .map(externalId -> format("CRM-%s", externalId))
                .collect(toSet());
    }
}
