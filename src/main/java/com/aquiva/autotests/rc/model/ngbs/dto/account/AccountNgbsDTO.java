package com.aquiva.autotests.rc.model.ngbs.dto.account;

import com.aquiva.autotests.rc.model.DataModel;
import com.aquiva.autotests.rc.model.ngbs.dto.AddressDTO;
import com.fasterxml.jackson.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Data object with account information for usage with NGBS API services.
 */
public class AccountNgbsDTO extends DataModel {
    public FreeServiceCreditDTO freeServiceCredit;
    public AccountPackageDTO[] packages;
    public String id;
    public String enterpriseAccountId;
    public Integer businessIdentityId;
    public Addresses addresses;

    /**
     * Get the main active package on the NGBS Account.
     * In most cases there's only one package anyway.
     */
    public AccountPackageDTO getMainPackage() {
        return packages[0];
    }

    /**
     * Inner data structure that stores information about Free Service Credit
     * on account for usage with NGBS API services.
     */
    public static class FreeServiceCreditDTO extends DataModel {
        public double amount;
    }

    /**
     * Inner data structure that stores information about package
     * on NGBS account.
     */
    public static class AccountPackageDTO extends DataModel {
        //  For 'previousBillingDate', 'billingStartDate', 'nextBillingDate' fields
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'");

        public String id;
        public String catalogId;
        public String version;
        public String description;
        public String product;
        public String billingStartDate;
        public String previousBillingDate;
        public String nextBillingDate;
        public String masterDuration;
        public PackageLimitsDTO[] packageLimits;
        public License[] licenses;

        /**
         * Get {@link #billingStartDate} as {@link LocalDate}.
         */
        @JsonIgnore
        public LocalDate getBillingStartDateAsLocalDate() {
            return LocalDate.parse(billingStartDate, DATE_FORMATTER);
        }

        /**
         * Get {@link #previousBillingDate} as {@link LocalDate}.
         */
        @JsonIgnore
        public LocalDate getPreviousBillingDateAsLocalDate() {
            return LocalDate.parse(previousBillingDate, DATE_FORMATTER);
        }

        /**
         * Get {@link #nextBillingDate} as {@link LocalDate}.
         */
        @JsonIgnore
        public LocalDate getNextBillingDateAsLocalDate() {
            return LocalDate.parse(nextBillingDate, DATE_FORMATTER);
        }

        /**
         * Inner data structure that stores information about package limits
         * on package.
         */
        public static class PackageLimitsDTO extends DataModel {
            //  For 'code' and 'packageLimitCode' fields
            public static final String MONTHLY_AMOUNT_LIMIT = "MONTHLY_AMOUNT_LIMIT";

            public String code;
            public String packageLimitCode;
            public Long value;
        }

        /**
         * Inner data structure that stores information about licenses
         * on the NGBS account's package.
         */
        @JsonInclude(value = NON_NULL)
        public static class License extends DataModel {
            public String id;
            public String catalogId;
            public String description;
            public Integer qty;
            public License[] subLicenses;
        }
    }

    /**
     * Inner data structure that stores information about addresses
     * on the NGBS account (e.g. Billing Address).
     */
    public static class Addresses extends DataModel {

        @JsonProperty("Billing")
        public AddressDTO billingAddress;

        @JsonProperty("ServiceLocation")
        public AddressDTO serviceLocationAddress;
    }
}
