package com.aquiva.autotests.rc.utilities.salesforce;

import static com.aquiva.autotests.rc.utilities.Constants.BASE_URL;
import static com.aquiva.autotests.rc.utilities.Constants.SFDC_API_VERSION;

/**
 * Helper class for {@link SalesforceRestApiClient} class to store and form useful data.
 */
public class SalesforceRestApiHelper {

    //  WSDL endpoints
    private static final String SOAP_WSDL_ENTERPRISE = "/soap/wsdl.jsp?type=enterprise";
    private static final String SOAP_WSDL_TOOLING = "/services/wsdl/tooling";

    //  Standard REST API services
    private static final String STANDARD_SFDC_SERVICE_PREFIX = "/services/data/v" + SFDC_API_VERSION;
    private static final String ORG_LIMITS_SERVICE = STANDARD_SFDC_SERVICE_PREFIX + "/limits";

    //  Custom Apex REST API services
    private static final String SETTINGS_SERVICE = "/services/apexrest/SettingsService";

    /**
     * Return string URL for request to
     * <i>{salesforce.base.url}/soap/wsdl.jsp?type=enterprise</i>.
     *
     * @return string representation for URL to download Enterprise WSDL from the org
     */
    public static String getSoapWsdlEnterpriseURL() {
        return BASE_URL + SOAP_WSDL_ENTERPRISE;
    }

    /**
     * Return string URL for request to
     * <i>{salesforce.base.url}/services/soap/tooling</i>.
     *
     * @return string representation for URL to download Tooling WSDL from the org
     */
    public static String getSoapWsdlToolingURL() {
        return BASE_URL + SOAP_WSDL_TOOLING;
    }

    /**
     * Return string URL for request to
     * <i>{salesforce.base.url}/services/data/v{api.version}/limits</i>.
     *
     * @return string representation for URL to get the current limits of the org
     */
    public static String getOrgLimitsURL() {
        return BASE_URL + ORG_LIMITS_SERVICE;
    }

    /**
     * Return string URL for request to
     * <i>{salesforce.base.url}/services/apexrest/ServiceSettings</i>.
     *
     * @return string representation for URL to download various settings from SFDC
     * (feature toggles, custom settings, user info, account info, profile info, etc.)
     */
    public static String getSettingsServiceURL() {
        return BASE_URL + SETTINGS_SERVICE;
    }
}
