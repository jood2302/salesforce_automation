package com.aquiva.autotests.rc.utilities;

/**
 * Authentication parameters for connecting to REST API services.
 */
public class RestApiAuthentication {
    public String username;
    public String password;
    public String tokenKey;
    public String tokenValue;

    public Method method;

    /**
     * Create an "empty" authentication object, for services that don't require authentication.
     *
     * @return authentication parameters for REST API client without any authentication data
     */
    public static RestApiAuthentication usingNoAuthentication() {
        var noAuth = new RestApiAuthentication();
        noAuth.method = Method.NONE;

        return noAuth;
    }

    /**
     * Create new authentication data for Basic Authentication method (username and password).
     * <br/>
     * REST API client will add the following authorization header to its requests:
     * <br/>
     * {@code "Authorization: Basic {{username_and_password_combined_encoded_in_base64}}"}
     *
     * @param username username to use in the Basic Authentication
     * @param password password to use in the Basic Authentication
     * @return authentication parameters for REST API client with username and password
     */
    public static RestApiAuthentication usingBasicAuthentication(String username, String password) {
        var basicAuthAuthorization = new RestApiAuthentication();
        basicAuthAuthorization.method = Method.BASIC_AUTH;
        basicAuthAuthorization.username = username;
        basicAuthAuthorization.password = password;

        return basicAuthAuthorization;
    }

    /**
     * Create new authorization data for Authentication with API Key (token).
     * <br/>
     * REST API client will add the following authorization header to its requests:
     * <br/>
     * {@code "Authorization: {{API_Key}} {{API_Value}}"}
     *
     * @param tokenKey   string to use as a key for Token
     * @param tokenValue string to use as a value for Token
     * @return authentication parameters for REST API client with token key/value pair
     */
    public static RestApiAuthentication usingApiKey(String tokenKey, String tokenValue) {
        var tokenAuthorization = new RestApiAuthentication();
        tokenAuthorization.method = Method.API_KEY;
        tokenAuthorization.tokenKey = tokenKey;
        tokenAuthorization.tokenValue = tokenValue;

        return tokenAuthorization;
    }

    /**
     * Authentication methods (HTTP Authentication Schemes, API Keys, etc.).
     */
    public enum Method {
        NONE,
        BASIC_AUTH,
        API_KEY
    }
}
