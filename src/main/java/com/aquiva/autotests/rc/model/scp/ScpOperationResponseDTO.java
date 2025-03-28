package com.aquiva.autotests.rc.model.scp;

import com.aquiva.autotests.rc.model.DataModel;
import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Data object with response information for usage with SCP API services.
 */
@JsonInclude(value = NON_NULL)
public class ScpOperationResponseDTO extends DataModel {
    public Extensions extensions;
    public Error[] errors;
    public Data data;

    /**
     * Data object that represents response's service data (id, timestamp).
     */
    @JsonInclude(value = NON_NULL)
    public static class Extensions extends DataModel {
        public String requestId;
        public String timestamp;

        //  For errors only
        public String status;
        public String code;
        public String classification;
    }

    /**
     * Data object that represents error details in the response (if there's any).
     */
    @JsonInclude(value = NON_NULL)
    public static class Error extends DataModel {
        public String message;
        public String[] path;
        public Extensions extensions;
    }

    /**
     * Data object that represents an actual data (e.g. account data)
     * that was returned inside the response.
     */
    @JsonInclude(value = NON_NULL)
    public static class Data extends DataModel {
        //  For 'account.userServiceParameterValues[].parameterId' field
        //  For 'config.serviceParameters[].parameterId' field
        public static final Integer ENABLE_PHONE_RENTAL_PARAM_ID = 537;

        public Account account;
        public AccountConfig config;

        /**
         * Data object that represents any account data
         * (personal, service, verification, etc.).
         */
        @JsonInclude(value = NON_NULL)
        public static class Account extends DataModel {
            public String[] testerFlags;
            public AccountInfo accountInfo;
            public UserServiceParameterValue[] userServiceParameterValues;

            /**
             * Data object that represents the account's data.
             */
            @JsonInclude(value = NON_NULL)
            public static class AccountInfo extends DataModel {
                public String accountId;
                public ServiceInfo serviceInfo;

                /**
                 * Data object that represents service data on the account
                 * (brand name, brand ID, tester flags, etc.).
                 */
                @JsonInclude(value = NON_NULL)
                public static class ServiceInfo extends DataModel {
                    public String[] testerFlags;
                }
            }

            /**
             * Data object that represents the values of account's 'Features & Settings'.
             */
            @JsonInclude(value = NON_NULL)
            public static class UserServiceParameterValue extends DataModel {
                public Integer parameterId;
                public Boolean boolValue;
            }
        }

        /**
         * Data object that represents the account's configuration settings.
         */
        @JsonInclude(value = NON_NULL)
        public static class AccountConfig extends DataModel {
            public UserServiceParameter[] serviceParameters;

            /**
             * Data object that represents the Service Parameters' description
             * (name, group name, dependencies, parameter ID, etc.)
             */
            @JsonInclude(value = NON_NULL)
            public static class UserServiceParameter extends DataModel {
                public Integer parameterId;
                public String name;
            }
        }
    }
}
