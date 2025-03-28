package com.aquiva.autotests.rc.model.funnel;

import com.aquiva.autotests.rc.model.DataModel;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Data object with the SignUp request for the Funnel API.
 * <br/>
 * Used by the Salesforce when sending information to the Funnel API when signing up
 * a New Business Account.
 */
public class SignUpBodyFunnelDTO extends DataModel {

    //	for 'elaType' field
    public static final String BILLING_ELA_TYPE = "Billing";
    public static final String SERVICE_ELA_TYPE = "Service";

    @JsonProperty("ELAType")
    public String elaType;

    @JsonProperty("SubscriptionInfo")
    public SubscriptionInfo subscriptionInfo;

    @JsonProperty("uBrandId")
    public String uBrandId;

    @JsonProperty("preferredUserLanguage")
    public String preferredUserLanguage;

    /**
     * Data object that represents the subscription information for the SignUp request.
     * <br/>
     * Used for the ELA Accounts.
     */
    public static class SubscriptionInfo extends DataModel {
        @JsonProperty("ELAContract")
        public ElaContract elaContract;

        @JsonProperty("LicenseCategories")
        public List<LicenseCategory> licenseCategories;

        /**
         * Data object that represents the ELA Contract's info for the ELA Account.
         */
        public static class ElaContract extends DataModel {
            public List<LicensesItem> licenses;
            public ElaSettings elaSettings;

            /**
             * Data object that represents the Licenses info for the ELA Account
             * (for ELA Type = 'Billing').
             */
            public static class LicensesItem extends DataModel {
                public String catalogId;
                public Integer contractualQty;
            }

            /**
             * Data object that represents the ELA Settings info (i.e. licenses) for the ELA Account
             * (for ELA Type = 'Service').
             */
            public static class ElaSettings extends DataModel {
                public List<ElaSettingsLicenseItem> licenses;

                /**
                 * Data object that represents the Licenses info for the ELA Account
                 * (for ELA Type = 'Service').
                 */
                public static class ElaSettingsLicenseItem extends DataModel {
                    public String catalogId;
                    public Integer maxQty;
                }
            }
        }

        /**
         * Data object that represents the License Category info for the Account.
         */
        public static class LicenseCategory extends DataModel {
            public String licenseCategory;
            public List<License> licenses;

            /**
             * Data object that represents the License info for the License Category.
             */
            public static class License extends DataModel {
                public String catalogId;
                public Integer qty;
            }
        }
    }
}
