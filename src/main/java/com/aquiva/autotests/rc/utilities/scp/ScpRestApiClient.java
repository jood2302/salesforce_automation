package com.aquiva.autotests.rc.utilities.scp;

import com.aquiva.autotests.rc.model.scp.ScpOperationRequestDTO;
import com.aquiva.autotests.rc.model.scp.ScpOperationRequestDTO.Variables.TesterFlagsItem;
import com.aquiva.autotests.rc.model.scp.ScpOperationResponseDTO;
import com.aquiva.autotests.rc.utilities.RestApiClient;
import io.qameta.allure.Step;
import org.apache.http.message.BasicHeader;

import java.util.List;

import static com.aquiva.autotests.rc.model.scp.ScpOperationRequestDTO.*;
import static com.aquiva.autotests.rc.model.scp.ScpOperationRequestDTO.Variables.TesterFlagsItem.*;
import static com.aquiva.autotests.rc.utilities.RestApiAuthentication.usingApiKey;
import static com.aquiva.autotests.rc.utilities.RestApiClient.ACCEPT_ENCODING_HEADER;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.aquiva.autotests.rc.utilities.scp.ScpRestApiHelper.*;

/**
 * Class for handling calls to SCP API (AWU backend service).
 * <br/>
 * Useful for getting the data on NGBS accounts like RC Tester Flags.
 */
public class ScpRestApiClient {
    private static final RestApiClient CLIENT = new RestApiClient(
            usingApiKey(SCP_TOKEN_KEY, SCP_TOKEN_VALUE),
            "Unable to get a response from SCP! Details: "
    );

    /*  
        We need to remove the default "Accept-Encoding": "gzip" header, 
        otherwise we'll get the empty responses from SCP.
        We work with SCP via AWU (it's easier), 
        but all the other clients work with AWU via AGW+AWP which does the encoding for it.
    */
    static {
        CLIENT.addHeaders(List.of(new BasicHeader(ACCEPT_ENCODING_HEADER, EMPTY_STRING)));
    }

    /**
     * Get all the Tester Flags' values on the NGBS account.
     *
     * @param rcUserId RC User ID ("Enterprise ID" in BAP) or User ID (in SCP),
     *                 e.g. "400852991008".
     * @return response data that contains Tester Flags in it.
     * <p> Note: make sure to check for "errors" first, before checking anything in "data" in the response! </p>
     */
    @Step("Get Tester Flags on the NGBS account")
    public static ScpOperationResponseDTO getTesterFlagsOnAccount(String rcUserId) {
        var url = getRestApiGraphQlURL();

        var scpOperationRequest = new ScpOperationRequestDTO();
        scpOperationRequest.operationName = ACCOUNT_INFO_OPERATION;

        var variables = new Variables();
        variables.userId = rcUserId;
        scpOperationRequest.variables = variables;

        scpOperationRequest.query = GET_TESTER_FLAGS_QUERY;

        return CLIENT.post(url, scpOperationRequest, ScpOperationResponseDTO.class);
    }

    /**
     * Remove Tester Flags on the NGBS account.
     * <br/>
     * "Removing" = changing the list of flags to remove values like "Tester", "Auto-delete",
     * "Send Real Requests to Zoom" and "Send Real Requests to Distributor" from there.
     *
     * @param rcUserId RC User ID ("Enterprise ID" in BAP) or User ID (in SCP),
     *                 e.g. "400852991008".
     * @return response data after request to change Tester Flags.
     * <p> Note: make sure to check for "errors" first, before checking anything in "data" in the response! </p>
     */
    @Step("Remove Tester Flags on the NGBS account")
    public static ScpOperationResponseDTO removeTesterFlagsOnAccount(String rcUserId) {
        var url = getRestApiGraphQlURL();

        var scpOperationRequest = new ScpOperationRequestDTO();
        scpOperationRequest.operationName = CHANGE_TESTER_FLAGS_OPERATION;

        var variables = new Variables();
        variables.accountId = rcUserId;
        variables.comment = "Changed by QA Automation script";
        variables.testerFlags = new TesterFlagsItem[]{
                new TesterFlagsItem(TESTER, false),
                new TesterFlagsItem(AUTO_DELETE, false),
                new TesterFlagsItem(KEEP_FOR_UP_TO_30_DAYS_AFTER_SIGNUP, false),
                new TesterFlagsItem(SEND_REAL_REQUESTS_TO_ZOOM, false),
                new TesterFlagsItem(SEND_REAL_REQUESTS_TO_DISTRIBUTOR, false),
                new TesterFlagsItem(NO_EMAIL_NOTIFICATIONS, true)
        };
        scpOperationRequest.variables = variables;

        scpOperationRequest.query = REMOVE_TESTER_FLAGS_QUERY;

        return CLIENT.post(url, scpOperationRequest, ScpOperationResponseDTO.class);
    }

    /**
     * Get all the info about Service Parameters on the NGBS account.
     * Service Parameters are various settings and features on the account,
     * like enterprise features, calling features, messaging feature, security settings, etc.
     *
     * @param rcUserId RC User ID ("Enterprise ID" in BAP) or User ID (in SCP),
     *                 e.g. "400852991008".
     * @return response data that contains Service Parameters in it.
     * <p> Note: make sure to check for "errors" first, before checking anything in "data" in the response! </p>
     */
    @Step("Get Service Parameters on the NGBS account")
    public static ScpOperationResponseDTO getServiceParameters(String rcUserId) {
        var url = getRestApiGraphQlURL();

        var scpOperationRequest = new ScpOperationRequestDTO();
        scpOperationRequest.operationName = SERVICE_PARAMETERS_OPERATION;

        var variables = new Variables();
        variables.userId = rcUserId;
        scpOperationRequest.variables = variables;

        scpOperationRequest.query = GET_SERVICE_PARAMETERS;

        return CLIENT.post(url, scpOperationRequest, ScpOperationResponseDTO.class);
    }
}
