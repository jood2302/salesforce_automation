package com.aquiva.autotests.rc.utilities.salesforce;

import com.sforce.soap.enterprise.EnterpriseConnection;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.tooling.ToolingConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

import static com.aquiva.autotests.rc.utilities.Constants.*;

/**
 * Factory class for producing instances of API connection objects.
 * These objects are used for interaction with Salesforce API.
 * <p></p>
 * Supported object types are:
 * <p> - Enterprise Connection (for most operations with SFDC objects) </p>
 * <p> - Tooling Connection (for access to metadata; for invoking Apex code) </p>
 */
public class ConnectionFactory {

    /**
     * URL Endpoint for SOAP API connection.
     */
    private static final String ENDPOINT_SOAP = "https://%s/services/Soap/c/%s/";

    /**
     * Default configuration parameters for connection via Salesforce API.
     */
    private static final SalesforceConfig SALESFORCE_CONFIG = new SalesforceConfig(USER, PASSWORD, SECURITY_TOKEN, LOGIN_URL);

    //  ### Enterprise Connection ###

    /**
     * Get Enterprise connection object using the default configuration.
     *
     * @return API Connection object for interaction with SFDC via SOAP API
     * @throws ConnectionException in case of errors while accessing API
     * @see ConnectionFactory#SALESFORCE_CONFIG
     */
    public static EnterpriseConnection getDefaultEnterpriseConnection() throws ConnectionException {
        return getEnterpriseConnection(SALESFORCE_CONFIG);
    }

    /**
     * Get Enterprise connection object using a custom provided configuration.
     *
     * @param config configuration parameters for establishing API connection with SFDC
     * @return API Connection for interaction with SFDC via SOAP API
     * @throws ConnectionException in case of errors while accessing API
     */
    public static EnterpriseConnection getEnterpriseConnection(SalesforceConfig config) throws ConnectionException {
        var authEndpoint = String.format(ENDPOINT_SOAP, config.serverUrl, SFDC_API_VERSION);

        var enterpriseConfig = new ConnectorConfig();
        enterpriseConfig.setServiceEndpoint(authEndpoint);
        enterpriseConfig.setAuthEndpoint(authEndpoint);
        enterpriseConfig.setUsername(config.username);
        enterpriseConfig.setPassword(config.password);
        enterpriseConfig.setManualLogin(false);

        return com.sforce.soap.enterprise.Connector.newConnection(enterpriseConfig);
    }

    //  ### Tooling Connection ###

    /**
     * Get Tooling connection object using the default configuration.
     *
     * @return API Connection object for interaction with SFDC via SOAP API
     * @throws ConnectionException in case of errors while accessing API
     * @see ConnectionFactory#SALESFORCE_CONFIG
     */
    public static ToolingConnection getDefaultToolingConnection() throws ConnectionException {
        return getToolingConnection(SALESFORCE_CONFIG);
    }

    /**
     * Get Tooling connection object using a custom provided configuration.
     *
     * @param config configuration parameters for establishing API connection with SFDC
     * @return API Connection for interaction with SFDC via SOAP API
     * @throws ConnectionException in case of errors while accessing API
     */
    public static ToolingConnection getToolingConnection(SalesforceConfig config) throws ConnectionException {
        //  Need to get Enterprise Connection first
        //  to get valid Session ID and Service Endpoint values for the Tooling's ConnectorConfig
        var connection = getEnterpriseConnection(config);

        var toolingConfig = new ConnectorConfig();
        toolingConfig.setSessionId(connection.getSessionHeader().getSessionId());

        var toolingServiceEndpoint = connection.getConfig().getServiceEndpoint()
                .replaceAll("Soap/\\w/", "Soap/T/");
        toolingConfig.setServiceEndpoint(toolingServiceEndpoint);

        toolingConfig.setManualLogin(false);

        return com.sforce.soap.tooling.Connector.newConnection(toolingConfig);
    }

    //  ### Metadata Connection ###

    /**
     * Get Metadata connection object using the default configuration.
     *
     * @return API Connection object for interaction with SFDC via SOAP API
     * @throws ConnectionException in case of errors while accessing API
     * @see ConnectionFactory#SALESFORCE_CONFIG
     */
    public static MetadataConnection getDefaultMetadataConnection() throws ConnectionException {
        return getMetadataConnection(SALESFORCE_CONFIG);
    }

    /**
     * Get Metadata connection object using a custom provided configuration.
     *
     * @param config configuration parameters for establishing API connection with SFDC
     * @return API Connection for interaction with SFDC via SOAP API
     * @throws ConnectionException in case of errors while accessing API
     */
    public static MetadataConnection getMetadataConnection(SalesforceConfig config) throws ConnectionException {
        //  Need to get Enterprise Connection first
        //  to get valid Session ID and Service Endpoint values for the Metadata's ConnectorConfig
        var connection = getEnterpriseConnection(config);

        var metadataConfig = new ConnectorConfig();
        metadataConfig.setSessionId(connection.getSessionHeader().getSessionId());

        var metadataServiceEndpoint = connection.getConfig().getServiceEndpoint()
                .replaceAll("Soap/\\w/", "Soap/m/");
        metadataConfig.setServiceEndpoint(metadataServiceEndpoint);

        metadataConfig.setManualLogin(false);

        return com.sforce.soap.metadata.Connector.newConnection(metadataConfig);
    }
}
