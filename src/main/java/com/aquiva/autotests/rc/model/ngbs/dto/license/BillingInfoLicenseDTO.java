package com.aquiva.autotests.rc.model.ngbs.dto.license;

import com.aquiva.autotests.rc.model.DataModel;

/**
 * Data object with licenses on account information for usage with NGBS API services.
 *
 * <p></p>
 * It is used to contain information about licenses on the account.
 * <p></p>
 * Normally, this object consists of:
 * <p> - catalog id for license </p>
 * <p> - quantity of license(s) </p>
 * <p> - description license (DigitalLine Unlimited, Mobile User, etc. ) </p>
 * <p> - billing cycle duration (Annual, Monthly)</p>
 * <p> - billing type (Recurring, Usage, etc.)</p>
 */
public class BillingInfoLicenseDTO extends DataModel {
    //  For 'billingType' field
    public static final String RECURRING_BILLING_TYPE = "Recurring";
    public static final String USAGE_BILLING_TYPE = "Usage";

    //  For 'billingCycleDuration' field
    public static final String ANNUAL_BILLING_CYCLE_DURATION = "Annual";
    public static final String MONTHLY_BILLING_CYCLE_DURATION = "Monthly";

    public String catalogId;
    public Integer qty;
    public String description;
    public String billingCycleDuration;
    public String billingType;
}

