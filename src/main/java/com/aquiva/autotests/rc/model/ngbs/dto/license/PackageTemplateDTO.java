package com.aquiva.autotests.rc.model.ngbs.dto.license;

import com.aquiva.autotests.rc.model.DataModel;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Data object with package information for usage with NGBS API services.
 * <br/>
 * It is used to contain information about packages on the account
 * when adding new packages to the existing account.
 */
@JsonInclude(value = NON_NULL)
public class PackageTemplateDTO extends DataModel {

    //	For 'productLine' field
    public static final String ENGAGE_PRODUCT_LINE = "Engage";
    public static final String CONTACT_CENTER_PRODUCT_LINE = "Contact Center";
    public static final String PROFESSIONAL_SERVICES_PRODUCT_LINE = "Professional Services";

    //	For 'product' field
    public static final String DEFAULT_PRODUCT = "9";

    //  For 'masterDuration' field
    public static final String ANNUAL_MASTER_DURATION = "Annual";
    public static final String MONTHLY_MASTER_DURATION = "Monthly";

    //	For 'status' field
    public static final String INITIAL_PACKAGE_STATUS = "Initial";

    public String catalogId;
    public String version;
    public String productLine;
    public String product;
    public String serviceAccountId;
    public String masterDuration;
    public String billingStartDate;
    public String status;
    public List<CatalogItem> catalogLicenses;
}