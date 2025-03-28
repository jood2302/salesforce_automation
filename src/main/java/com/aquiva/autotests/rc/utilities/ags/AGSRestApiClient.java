package com.aquiva.autotests.rc.utilities.ags;

import com.aquiva.autotests.rc.model.ags.AccountAgsDTO;
import com.aquiva.autotests.rc.model.ags.JobResultInfoDTO;
import com.aquiva.autotests.rc.utilities.RestApiClient;
import io.qameta.allure.Step;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.aquiva.autotests.rc.utilities.RestApiAuthentication.usingNoAuthentication;
import static com.aquiva.autotests.rc.utilities.ags.AGSRestApiHelper.*;
import static java.lang.String.format;

/**
 * Class for handling calls to AGS API (Account Generation System).
 * <br/>
 * Useful for generating accounts in NGBS on-the-fly
 * to use in the tests related to the Existing Business functionality.
 */
public class AGSRestApiClient {
    private static final RestApiClient CLIENT = new RestApiClient(
            usingNoAuthentication(),
            "Error while interacting with AGS! Details: ");

    private static final Logger LOG = LoggerFactory.getLogger(AGSRestApiClient.class);

    //  Constants for polling job results service
    private static final long JOB_STATUS_TIMEOUT = 
            Long.parseLong(System.getProperty("ags.create.timeout", "420")) * 1000;
    private static final long POLLING_INTERVAL = 3_000L;

    /**
     * Generate a new account in NGBS via AGS.
     * <p></p>
     * Account creation is done using special scenarios with instructions to API.
     * These instructions consist of desired parameters for created account,
     * like specific package and/or brand, number of digital lines, and many more.
     * <p></p>
     * More info on scenarios in RC Wiki:
     * <a href=https://wiki.ringcentral.com/display/RBS/Billing+AGS+Scenarios>Billing AGS Scenarios</a>.
     *
     * @param scenario scenario for AGS API to create account with parameters.
     *                 <p> E.g. <b>"ngbs(brand=1210,package=1231005v2,dlCount=30)"</b> for: </p>
     *                 <p> - RingEX Core™ package (packageID = 1231005, version = 2) </p>
     *                 <p> - 30 digital lines </p>
     * @return {@link AccountAgsDTO} object which is deserialized version of JSON response from AGS API
     * after successful account creation.
     * @throws RuntimeException if timeout for job status waiting is exceeded, or job has failed,
     *                          or if there's an error returned from AGS services (4xx, 5xx response codes).
     */
    @Step("Generate a new account in NGBS via AGS REST API")
    public static AccountAgsDTO createAccount(String scenario) {
        var jobId = generateNewAccount(scenario);

        //  Below is just an implementation of interval polling of "jobStatus" service without using any Thread.sleep()
        var start = System.currentTimeMillis();

        String jobStatus;
        while (!(jobStatus = checkJobStatus(jobId)).equalsIgnoreCase("SUCCESS")) {
            var cycleStart = System.currentTimeMillis();

            if (System.currentTimeMillis() - start > JOB_STATUS_TIMEOUT) {
                throw new RuntimeException("Timeout exceeded getting 'SUCCESS' job status! " +
                        "Current job status = " + jobStatus);
            } else if (jobStatus.equalsIgnoreCase("FAILED")) {
                var errorDetails = getJobResultErrorDescription(jobId);
                throw new RuntimeException(
                        format("Current AGS job with id=%d failed! \n" +
                                        "Please review your AGS scenario '%s' in test data or check AGS availability.\n" +
                                        "Additional details from AGS: \n'%s'",
                                jobId, scenario, errorDetails));
            }

            while (true) {
                if (System.currentTimeMillis() - cycleStart > POLLING_INTERVAL) {
                    break;
                }
            }
        }

        var accountInfo = getGeneratedAccountInfo(jobId);
        LOG.info(String.format("NGBS Account created via AGS: billingId = %s, packageId = %s",
                accountInfo.getAccountBillingId(), accountInfo.getAccountPackageId()));

        return accountInfo;
    }

    /**
     * Start a new job for account generation via AGS API.
     * <p></p>
     * First step in generating new account in NGBS and getting its data.
     *
     * @param scenario special scenario for account generation (e.g. "ngbs(brand=1210,package=1231005v2)")
     * @return job ID to track account generation status
     */
    private static long generateNewAccount(String scenario) {
        var url = getAccountGenerateURL();
        var jsonBody = getAccountGenerateJsonBody(scenario);
        var response = CLIENT.post(url, jsonBody);
        return new JSONObject(response).getLong("job");
    }

    /**
     * Check the account generation's job status by job ID.
     * Typically, AGS API returns a response with the job status, like:
     * IN_PROGRESS, SUCCESS, FAILED or UNKNOWN.
     * <p></p>
     * Intermediate step in the generating new account in NGBS and getting its data.
     *
     * @param jobId ID of job for account generation (e.g. 93744, 84347, etc...)
     * @return current job status (e.g. "IN_PROGRESS")
     */
    private static String checkJobStatus(long jobId) {
        var url = getJobStatusURL(jobId);
        var response = CLIENT.get(url);
        return new JSONObject(response).getString("status");
    }

    /**
     * Get the additional error details in case of the failed job.
     *
     * @param jobId ID of job for account generation (e.g. 93744, 84347, etc...)
     * @return failed job's error description (e.g. "AGException: Invalid scenario format...")
     */
    private static String getJobResultErrorDescription(long jobId) {
        var url = getJobResultInfoURL(jobId);
        var jobResultInfo = CLIENT.get(url, JobResultInfoDTO.class);
        return jobResultInfo.getMainJob().error;
    }

    /**
     * Get all the info about the generated account by job ID.
     * <p></p>
     * AGS API responds with the detailed JSON object
     * that is deserialized into {@link AccountAgsDTO} object for further use in tests.
     * <p></p>
     * Final step in the generating new account in NGBS and getting its data.
     *
     * @param jobId ID of job for account generation (e.g. 93744, 84347, etc...)
     * @return Account data object that holds lots of useful data for tests
     * (billing and package IDs, RC User ID, first name and last name, email...)
     */
    private static AccountAgsDTO getGeneratedAccountInfo(long jobId) {
        var url = getJobResultURL(jobId);
        var generatedAccounts = CLIENT.getAsList(url, AccountAgsDTO.class);
        return generatedAccounts.get(0);
    }
}
