package com.aquiva.autotests.rc.utilities.ags;

import static com.aquiva.autotests.rc.utilities.Constants.BASE_ENV_NAME;
import static com.aquiva.autotests.rc.utilities.Constants.QA_SANDBOX_NAME;
import static java.lang.String.format;

/**
 * Helper class for {@link AGSRestApiClient} class to store and form useful data.
 */
public class AGSRestApiHelper {

    /**
     * Base URL for AGS REST API services (in case there's a need for testing in <i>specific</i> environment).
     * Used in dynamic account generation in Billing.
     * Should be controlled via system property/environment runtime variable.
     */
    private static final String BASE_AGS_REST_API_URL = System.getProperty("ags.rest.baseUrl");

    //  ### Environment URLs ###

    private static final String BASE_URL_GCI = "http://arm01-t01-ags01.int.rclabenv.com:8081";
    private static final String BASE_URL_BISUAT = "http://bis03-p01-ags01.int.rclabenv.com:8081";
    private static final String BASE_URL_PATCH = "http://biq01-t01-ags01.lab.nordigy.ru:8081";
    private static final String BASE_URL_DEV = "http://swt11-p01-ags01.lab.nordigy.ru:8081";

    //  ### Services paths ###

    private static final String REST_API_PATH = "/ag/api/v1";

    private static final String GENERATE_PATH = "/generate";
    private static final String GET_JOB_STATUS_PATH = "/getJobStatus?job=";
    private static final String GET_JOB_RESULT_PATH = "/getJobResult?job=";
    private static final String GET_JOB_RESULT_INFO_PATH = "/getJobResultInfo?job=";

    private static final String ACCOUNT_GENERATE_DEFAULT_PAYLOAD = "{\"batch\":[{\"scenario\": \"%s\", \"count\": \"1\", \"subsetName\":\"\"}]}";

    /**
     * Get "base" URL for AGS REST API.
     * This base URL is a common starting point for other services.
     *
     * @return base URL for other services (e.g. "http://arm01-t01-jws01.int.rclabenv.com/")
     */
    public static String getBaseAgsRestApiUrl() {
        if (BASE_AGS_REST_API_URL != null && !BASE_AGS_REST_API_URL.isBlank()) {
            return BASE_AGS_REST_API_URL;
        }

        if (BASE_ENV_NAME != null && !BASE_ENV_NAME.isBlank()) {
            switch (BASE_ENV_NAME) {
                default:
                case "ARM-BI-AMS":
                    return BASE_URL_GCI;
                case "SWT-UP-AMS":
                    return BASE_URL_DEV;
                case "BI-QA-SV7":
                    return BASE_URL_PATCH;
                case "BIS-UAT-SV7":
                    return BASE_URL_BISUAT;
            }
        }

        switch (QA_SANDBOX_NAME.toUpperCase()) {
            default:
            case "GCI":
                return BASE_URL_GCI;
            case "BISUAT":
                return BASE_URL_BISUAT;
            case "PATCH":
                return BASE_URL_PATCH;
        }
    }

    /**
     * Return string URL for request to
     * <i>{ags.api.endpoint}/ags/rest/api/v1/generate</i> (or something similar).
     *
     * @return string representation for URL to start jobs for account generation.
     */
    public static String getAccountGenerateURL() {
        return getBaseAgsRestApiUrl() + REST_API_PATH + GENERATE_PATH;
    }

    /**
     * Return string URL for request to
     * <i>{ags.api.endpoint}/ags/rest/api/v1/getJobStatus?job={jobId}</i> (or something similar).
     *
     * @param jobId ID of job for account generation (e.g. 93744, 84347, etc...)
     * @return string representation for URL to request AGS job status
     */
    public static String getJobStatusURL(long jobId) {
        return getBaseAgsRestApiUrl() + REST_API_PATH + GET_JOB_STATUS_PATH + jobId;
    }

    /**
     * Return string URL for request to
     * <i>{ags.api.endpoint}/ags/rest/api/v1/getJobResult?job={jobId}</i> (or something similar).
     *
     * @param jobId ID of job for account generation (e.g. 93744, 84347, etc...)
     * @return string representation for URL to request AGS job full results (with account info)
     */
    public static String getJobResultURL(long jobId) {
        return getBaseAgsRestApiUrl() + REST_API_PATH + GET_JOB_RESULT_PATH + jobId;
    }

    /**
     * Return string URL for request to
     * <i>{ags.api.endpoint}/ags/rest/api/v1/getJobResultInfo?job={jobId}</i> (or something similar).
     *
     * @param jobId ID of job for account generation (e.g. 93744, 84347, etc...)
     * @return string representation for URL to request AGS job additional results (with error description)
     */
    public static String getJobResultInfoURL(long jobId) {
        return getBaseAgsRestApiUrl() + REST_API_PATH + GET_JOB_RESULT_INFO_PATH + jobId;
    }

    /**
     * Get body/payload for account generation request.
     *
     * @param scenario special scenario for account generation (e.g. "ngbs(brand=1210,package=1231005v2)").
     *                 It should be integrated into JSON body of the request.
     * @return string representation for JSON body for POST Request to AGS API
     */
    public static String getAccountGenerateJsonBody(String scenario) {
        return format(ACCOUNT_GENERATE_DEFAULT_PAYLOAD, scenario);
    }
}
