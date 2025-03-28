package com.aquiva.autotests.rc.utilities.testit;

import static java.lang.String.format;

/**
 * Helper class for {@link TestItRestApiClient} class to store and form useful data.
 */
public class TestItRestApiHelper {

    //  TestIt base url
    private static final String TEST_IT_BASE_URL = "https://testit.ringcentral.com";

    //  Test case information
    private static final String TEST_CASE_INFO = "/api/v1/case/%s";

    //  Test case list
    private static final String SEARCH_TEST_CASE_LIST = "/api/v1/search/cases/list";

    //  Request bodies
    //  Note:
    //  - for "projectIds" (Project): 1361 = "Customer Relationship Management"
    //  - for "execution_type" (Execution Type): 1 = "Automated", 4 = "Ready for Automation"
    //  - for "custom_value_id" (Labels): 34993818 = "SIT", 35010070 = "Team Vira", 35683641 = "Team Tejas"
    public static final String SEARCH_AUTOMATED_TEST_CASES_QUERY =
            """
                    {
                        "projectIds": [ 1361 ],
                        "query": {
                          "operator": "and",
                          "queries": [
                            { "operator": "in", "property": "execution_type", "value": [ 1, 4 ] },
                            { "operator": "=", "property": "is_active", "value": true },
                            { "operator": "not", "query": { "operator": "~", "property": "suite_name", "value": "JUNK"} },
                            { "operator": "not", "query": { "operator": "~", "property": "suite_name", "value": "IT CRM"} },
                            { "operator": "not", "query": { "operator": "in", "property": "custom_value_id", "value": [ 34993818, 35010070, 35683641 ]} }
                          ]
                        }
                      }
                      """;

    /**
     * Return string URL for request to
     * <i>{testit.api.baseUrl}/api/v1/case/{testCaseId}</i>
     *
     * @return string representation for URL to get test case information
     */
    public static String getTestCaseInfoURL(String testCaseId) {
        return TEST_IT_BASE_URL + format(TEST_CASE_INFO, testCaseId);
    }

    /**
     * Return string URL for request to
     * <i>{testit.api.baseUrl}/api/v1/search/cases/list</i>
     *
     * @return string representation for URL to get test cases list
     */
    public static String getTestCaseListURL() {
        return TEST_IT_BASE_URL + SEARCH_TEST_CASE_LIST;
    }
}
