package com.aquiva.autotests.rc.utilities.salesforce;

/**
 * Configuration parameters for connecting to SFDC via SOAP API.
 */
public class SalesforceConfig {
    public final String username;
    public final String password;
    public final String serverUrl;

    /**
     * Parameterized constructor for configuration object.
     *
     * @param username      login/username of registered SFDC user
     * @param password      password of registered SFDC user
     * @param securityToken security token of registered SFDC user
     *                      (you can reset it in SFDC in User's settings and get it via email)
     * @param serverUrl     URL for Salesforce server used for authentication
     *                      (e.g. "test.salesforce.com" for sandbox; "login.salesforce.com" for production)
     */
    public SalesforceConfig(String username, String password, String securityToken, String serverUrl) {
        this.username = username;
        this.password = password + securityToken;
        this.serverUrl = serverUrl;
    }
}
