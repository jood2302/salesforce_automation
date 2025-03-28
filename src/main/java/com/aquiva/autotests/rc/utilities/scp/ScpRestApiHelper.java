package com.aquiva.autotests.rc.utilities.scp;

import com.aquiva.autotests.rc.utilities.JsonUtils;
import org.json.JSONObject;

import static com.aquiva.autotests.rc.utilities.Constants.BASE_ENV_NAME;
import static com.aquiva.autotests.rc.utilities.Constants.QA_SANDBOX_NAME;

/**
 * Helper class for {@link ScpRestApiClient} class to store and form useful data.
 */
public class ScpRestApiHelper {
    /**
     * Base URL for SCP REST API services (in case there's a need for testing in <i>specific</i> environment).
     * Used in editing tester flags on NGBS accounts (among other things).
     * Should be controlled via system property/environment runtime variable.
     */
    private static final String BASE_SCP_REST_API_URL = System.getProperty("scp.rest.baseUrl");

    //  Authorization data (note: the token is valid for all environments)
    public static final String SCP_TOKEN_KEY = getScpTokenKey();
    public static final String SCP_TOKEN_VALUE = getScpTokenValue();

    //  SCP endpoints for different environments
    public static final String SCP_REST_ENDPOINT_GCI = "http://arm01-t01-awu-all.int.rclabenv.com:8080";
    public static final String SCP_REST_ENDPOINT_BISUAT = "http://bis03-p01-awu-all.int.rclabenv.com:8080";
    public static final String SCP_REST_ENDPOINT_DEV = "http://swt11-p01-awu01.lab.nordigy.ru:8080";
    public static final String SCP_REST_ENDPOINT_PATCH = "http://biq01-t01-awu01.lab.nordigy.ru:8080";

    //  Endpoints
    public static final String RESTAPI_GRAPHQL = "/restapi/graphql";

    //  Queries
    public static final String GET_TESTER_FLAGS_QUERY =
            "query AccountInfo($userId: ID!) {\n" +
                    "  account {\n" +
                    "    accountInfo(request: {userId: $userId}) {\n" +
                    "      serviceInfo {\n" +
                    "        testerFlags\n" +
                    "        }\n" +
                    "      }\n" +
                    "    }\n}" +
                    "\n";
    public static final String REMOVE_TESTER_FLAGS_QUERY =
            "mutation ChangeTesterFlags($accountId: ID!, $comment: String!, $testerFlags: [TesterFlagUpdate!]!) {\n" +
                    "  account {\n" +
                    "    testerFlags: changeTesterFlags(\n" +
                    "      request: {accountId: $accountId, comment: $comment, testerFlags: $testerFlags}\n" +
                    "    )\n" +
                    "    }\n" +
                    "}\n";
    public static final String GET_SERVICE_PARAMETERS =
            "query ServiceParameters($userId: ID!) {\n" +
                    "  account {\n" +
                    "    userServiceParameterValues(request: {userId: $userId}) {\n" +
                    "      parameterId\n" +
                    "      ... on BoolSpValue {\n" +
                    "        boolValue\n" +
                    "      }\n" +
                    "    }\n" +
                    "  }\n" +
                    "  config {\n" +
                    "    serviceParameters(request: {userId: $userId}) {\n" +
                    "      parameterId\n" +
                    "      name\n" +
                    "    }\n" +
                    "  }\n" +
                    "}\n";

    /**
     * Get the key for the common X-SCP-Token.
     *
     * @return token's key to include in the authorization header for SCP REST API requests.
     */
    public static String getScpTokenKey() {
        var credentials = JsonUtils.readResourceAsString("auth/scp_token.json");
        return new JSONObject(credentials).getString("key");
    }

    /**
     * Get the value for the common X-SCP-Token.
     *
     * @return token's value to include in the authorization header for SCP REST API requests.
     */
    public static String getScpTokenValue() {
        var credentials = JsonUtils.readResourceAsString("auth/scp_token.json");
        return new JSONObject(credentials).getString("value");
    }

    /**
     * Get "base" URL for SCP REST API (AWU service for SCP backend).
     * This base URL is a common starting point for other services.
     *
     * @return base URL for other services (e.g. "http://arm01-t01-awu01.int.rclabenv.com:8080")
     */
    public static String getBaseScpRestApiUrl() {
        if (BASE_SCP_REST_API_URL != null && !BASE_SCP_REST_API_URL.isBlank()) {
            return BASE_SCP_REST_API_URL;
        }

        if (BASE_ENV_NAME != null && !BASE_ENV_NAME.isBlank()) {
            switch (BASE_ENV_NAME) {
                default:
                case "ARM-BI-AMS":
                    return SCP_REST_ENDPOINT_GCI;
                case "BIS-UAT-SV7":
                    return SCP_REST_ENDPOINT_BISUAT;
                case "BI-QA-SV7":
                    return SCP_REST_ENDPOINT_PATCH;
                case "SWT-UP-AMS":
                    return SCP_REST_ENDPOINT_DEV;
            }
        }

        switch (QA_SANDBOX_NAME.toUpperCase()) {
            default:
            case "GCI":
                return SCP_REST_ENDPOINT_GCI;
            case "BISUAT":
                return SCP_REST_ENDPOINT_BISUAT;
            case "PATCH":
                return SCP_REST_ENDPOINT_PATCH;
        }
    }

    /**
     * Return string URL for request to
     * <i>{scp.rest.baseUrl}/restapi/graphql</i>
     *
     * @return string representation for URL to perform CRUD operations via SCP AWU GraphQL.
     */
    public static String getRestApiGraphQlURL() {
        return getBaseScpRestApiUrl() + RESTAPI_GRAPHQL;
    }
}
