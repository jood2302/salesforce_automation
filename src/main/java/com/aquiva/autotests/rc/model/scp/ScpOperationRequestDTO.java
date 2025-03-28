package com.aquiva.autotests.rc.model.scp;

import com.aquiva.autotests.rc.model.DataModel;
import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Data object with request information for usage with SCP API services.
 */
@JsonInclude(value = NON_NULL)
public class ScpOperationRequestDTO extends DataModel {
    //  For 'operationName' field
    public static final String ACCOUNT_INFO_OPERATION = "AccountInfo";
    public static final String CHANGE_TESTER_FLAGS_OPERATION = "ChangeTesterFlags";
    public static final String SERVICE_PARAMETERS_OPERATION = "ServiceParameters";

    public String operationName;
    public Variables variables;
    public String query;

    /**
     * Data object that represents variables for request's queries
     * (user or account IDs, comments, etc.).
     */
    @JsonInclude(value = NON_NULL)
    public static class Variables extends DataModel {
        public String userId;
        public String accountId;
        public TesterFlagsItem[] testerFlags;
        public String comment;

        /**
         * Data object that represents key/value pair for RC Tester Flags on the NGBS Account.
         * <p> Keys are string values like "TESTER_CHECKED" or "AUTO_DELETE". </p>
         * <p> Values are {@code true} or {@code false} (checked or unchecked flag). </p>
         */
        @JsonInclude(value = NON_NULL)
        public static class TesterFlagsItem extends DataModel {
            //  For 'key' field
            public static final String TESTER = "TESTER_CHECKED";
            public static final String AUTO_DELETE = "AUTO_DELETE";
            public static final String KEEP_FOR_UP_TO_30_DAYS_AFTER_SIGNUP = "KEEP_30_CHECKED";
            public static final String SEND_REAL_REQUESTS_TO_DISTRIBUTOR = "ABP_REQUEST_ENABLED";
            public static final String SEND_REAL_REQUESTS_TO_ZOOM = "ZOOM_CHECKED";
            public static final String NO_EMAIL_NOTIFICATIONS = "NO_EMAIL_NOTIFICATIONS";

            public String key;
            public Boolean value;

            /**
             * Parameterized constructor to create a tester flag DTO.
             *
             * @param key   specific tester flag key (e.g. "TESTER_CHECKED")
             * @param value boolean value for flag (e.g. true or false: checked or unchecked)
             */
            public TesterFlagsItem(String key, Boolean value) {
                this.key = key;
                this.value = value;
            }
        }
    }
}