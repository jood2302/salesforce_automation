package com.aquiva.autotests.rc.utilities.ngbs;

import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.NGBS_Settings__c;

import static com.aquiva.autotests.rc.utilities.Constants.BASE_ENV_NAME;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiHelper.*;

/**
 * Utility class that encapsulates the logic for obtaining NGBS REST API settings:
 * endpoint URL, username and password, paths services (account manager, catalog), etc...
 * <p></p>
 * The class is designed using Singleton pattern to be the "single point of contact" for users of NGBS API settings data.
 */
public class NgbsRestApiSettings {

    //  Single instance of the class
    private static final NgbsRestApiSettings INSTANCE = new NgbsRestApiSettings();

    //  NGBS API settings data variables
    private final String endpoint;
    private final String username;
    private final String password;
    private final String accountManagerPath;
    private final String catalogPath;
    private final String invoiceEnginePath;

    /**
     * Class constructor.
     * Contains all the necessary logic to get NGBS settings data from SFDC (i.e. from current test sandbox).
     */
    private NgbsRestApiSettings() {
        EnterpriseConnectionUtils enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        try {
            NGBS_Settings__c ngbsSettings = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, BaseEndpoint__c, Login__c, Password__c, " +
                            "AccountManagerPath__c, CatalogPath__c, InvoiceEnginePath__c " +
                            "FROM NGBS_Settings__c " +
                            "WHERE SetupOwner.Name = 'RingCentral'",
                    NGBS_Settings__c.class);

            endpoint = ngbsSettings.getBaseEndpoint__c();
            username = ngbsSettings.getLogin__c();
            password = ngbsSettings.getPassword__c();
            accountManagerPath = ngbsSettings.getAccountManagerPath__c();
            catalogPath = ngbsSettings.getCatalogPath__c();
            invoiceEnginePath = ngbsSettings.getInvoiceEnginePath__c();

        } catch (Exception e) {
            throw new RuntimeException("NGBS API Settings can't be retrieved! Details: " + e.getMessage());
        }
    }

    /**
     * Get the instance of {@link NgbsRestApiSettings} object.
     * Useful for classes that contain logic for NGBS API connection/operations.
     *
     * @return instance of {@link NgbsRestApiSettings} with endpoint, username, password data.
     */
    public static NgbsRestApiSettings getInstance() {
        return INSTANCE;
    }

    /**
     * Get NGBS API endpoint URL.
     *
     * @return string value for NGBS REST API endpoint (e.g. "http://185.23.251.118:8080").
     */
    public String getEndpoint() {
        if (BASE_ENV_NAME != null && !BASE_ENV_NAME.isBlank()) {
            switch (BASE_ENV_NAME) {
                default:
                case "ARM-BI-AMS":
                    return NGBS_REST_ENDPOINT_GCI;
                case "SWT-UP-AMS":
                    return NGBS_REST_ENDPOINT_DEV;
                case "BI-QA-SV7":
                    return NGBS_REST_ENDPOINT_PATCH;
                case "BIS-UAT-SV7":
                    return NGBS_REST_ENDPOINT_BISUAT;
            }
        }

        return endpoint;
    }

    /**
     * Get NGBS REST API username.
     *
     * @return string value for NGBS REST API username (e.g. "salesforce666").
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get NGBS REST API password.
     *
     * @return string value for NGBS REST API password (e.g. "sAleSfOrCe1365").
     */
    public String getPassword() {
        return password;
    }

    /**
     * Get NGBS REST API path to Account Manager service.
     *
     * @return string value for NGBS REST API path to Account Manager (e.g. "/restapi/account-manager/v1.0").
     */
    public String getAccountManagerPath() {
        return accountManagerPath;
    }

    /**
     * Get NGBS REST API path to Catalog service.
     *
     * @return string value for NGBS REST API path to Catalog (e.g. "/restapi/catalog/v1.0").
     */
    public String getCatalogPath() {
        return catalogPath;
    }

    /**
     * Get NGBS REST API path to Invoice Engine service.
     *
     * @return string value for NGBS REST API path to Invoice Engine (e.g. "/restapi/invoice-engine/v1.0").
     */
    public String getInvoiceEnginePath() {
        return invoiceEnginePath;
    }
}
