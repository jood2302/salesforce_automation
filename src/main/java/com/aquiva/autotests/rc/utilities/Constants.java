package com.aquiva.autotests.rc.utilities;

import static java.lang.Boolean.parseBoolean;

/**
 * Global constant storage.
 * <p></p>
 * Constants from this class are related to:
 * <p> - sandbox information (e.g. name) </p>
 * <p> - access credentials (e.g. username/password for SF) </p>
 * <p> - global toggles/keys for turning on/off test features (e.g. turn on/off dynamic account generation) </p>
 * <p> - URLs for direct access to main SF pages (e.g. login, logout, Visualforce, etc...)</p>
 * <p> etc... </p>
 */
public class Constants {

    //  ### Sandbox/environment information ###
    /**
     * Name for Salesforce sandbox environment (e.g. "gci", "bisuat", etc).
     */
    public static final String QA_SANDBOX_NAME = System.getProperty("sf.sandboxName");
    /**
     * An environment name used to get proper URLs to AGS and SCP.
     */
    public static final String BASE_ENV_NAME = System.getProperty("env.name");
    /**
     * Current Salesforce API version
     * (depends on the current Salesforce edition, e.g. Summer '24 => 61.0).
     */
    public static final String SFDC_API_VERSION = "59.0";
    /**
     * true, if the current environment is a Sandbox, false for the Production Org.
     */
    public static final boolean IS_SANDBOX = QA_SANDBOX_NAME != null && !QA_SANDBOX_NAME.isBlank();

    //  ### Credentials ###
    /**
     * Username to log in into Salesforce via web/API.
     */
    public static final String USER = System.getProperty("sf.username", "invalidUsername") +
            (IS_SANDBOX ? "." + QA_SANDBOX_NAME : "");
    /**
     * Password to log in into Salesforce via web/API.
     */
    public static final String PASSWORD = System.getProperty("sf.password", "invalidPassword");
    /**
     * Security token to log into Salesforce via API.
     */
    public static final String SECURITY_TOKEN = System.getProperty("sf.token", "invalidToken");
    /**
     * Password to log in into PRM Portal via web.
     */
    public static final String PRM_PASSWORD = System.getProperty("prm.password", "invalidPrmPassword");

    //  ### URLs/links ###
    /**
     * Base URL for Salesforce sandbox environment.
     */
    public static final String BASE_URL = "https://rc" + (IS_SANDBOX ? "--" + QA_SANDBOX_NAME + ".sandbox" : "") + ".my.salesforce.com";
    /**
     * Base URL for Salesforce Visualforce pages.
     * Can be used to access custom VF pages, using parameters, and/or page names.
     */
    public static final String BASE_VF_URL = "https://rc" + (IS_SANDBOX ? "--" + QA_SANDBOX_NAME + "--c.sandbox" : "--c") + ".vf.force.com";
    /**
     * Base URL for Salesforce PRM portal pages.
     * Can be used to access PRM pages, using parameters, and/or page names.
     */
    public static final String BASE_PORTAL_URL = "https://rc" + (IS_SANDBOX ? "--" + QA_SANDBOX_NAME + ".sandbox" : "") + ".my.site.com";
    /**
     * URL to log into Salesforce via Salesforce API.
     */
    public static final String LOGIN_URL = (IS_SANDBOX ? "test" : "login") + ".salesforce.com";
    /**
     * URL to log out of the current Salesforce session.
     */
    public static final String LOGOUT_LINK = BASE_URL + "/secur/logout.jsp";
    /**
     * Label of the default application.
     */
    public static final String DEFAULT_APP_LABEL = "Sales";

    //  ### Toggles ###
    /**
     * Is 'Generate Accounts Dynamically' via AGS/NGBS API enabled or not (default: disabled).
     * Should be controlled via system property/environment runtime variable.
     */
    public static final boolean IS_GENERATE_ACCOUNTS = parseBoolean(System.getProperty("generateAccounts", "false"));

    //  ### PostCopy Parameters ###
    /**
     * JSON string that stores data for setting up 'Custom Settings' in SFDC.
     * <p>
     * The structure of this JSON at the top level represents the 'CustomSettings' object, at the child level
     * specific settings are listed ('GW_Settings__c', 'EndpointsSettings__c', 'NGBS_Settings__c', 'CCB_Settings__c') which inside contain
     * parameters specific to each setting, e.g.
     * <pre>
     * {
     *   "GW_Settings__c": {
     *     "BaseEndpoint__c": "https://sf-funnel-armbiams.rclabenv.com"
     *   },
     *   "EndpointsSettings__c": {
     *     "UQTServerEndpoint__c": "https://rc-gci-armbiams.rclabenv.com"
     *   }
     *   "NGBS_Settings__c": {
     *     "BaseEndpoint__c": "http://185.23.251.124:8080",
     *     "Login__c": "salesforce",
     *     "Password__c": "salesforce",
     *     "AccountManagerPath__c": "/restapi/account-manager/v1.0",
     *     "InvoiceEnginePath__c": "/restapi/invoice-engine/v1.0",
     *     "CatalogPath__c": "/restapi/catalog/v1.0",
     *     "CostCenterNestingLevel__c": 4
     *   },
     *   "CCB_Settings__c": {
     *     "BaseEndpoint__c": "https://ccb.ext.aws91-l11.lab.engage.ringcentral.com/restapi/v1.0/ccb",
     *     "Login__c": "ccb",
     *     "Password__c": "ccb",
     *     "SignUpPath__c": "/sign-up",
     *     "TerminationPath__c": "/termination/enterprise-account-id"
     *   }
     * }
     * </pre>
     */
    public static final String CUSTOM_SETTINGS = System.getProperty("sf.customSettings");
}