package com.aquiva.autotests.rc.utilities.salesforce;

import com.aquiva.autotests.rc.model.salesforce.Limits;
import com.aquiva.autotests.rc.utilities.RestApiClient;
import com.sforce.soap.enterprise.sobject.CountryCodes__c;
import org.json.JSONException;
import org.json.JSONObject;

import static com.aquiva.autotests.rc.utilities.RestApiAuthentication.usingApiKey;
import static com.aquiva.autotests.rc.utilities.salesforce.SalesforceRestApiHelper.*;

/**
 * Class for handling calls to Salesforce API.
 * <br/>
 * Useful for getting the data from Salesforce that cannot be obtained via SOAP API or web interface.
 */
public class SalesforceRestApiClient {

    //  Note: a client with this type of authentication is only suitable for getting WSDL files from the org!
    //  Use SOAP API for any other tasks (e.g. CRUD operations with records, manipulating metadata, etc.)
    private static final RestApiClient CLIENT_COOKIE_AUTH = new RestApiClient(
            usingApiKey("Cookie", "sid=" + EnterpriseConnectionUtils.getInstance().getSessionId()),
            "Unable to get a response from Salesforce! Details: "
    );

    //  Note: a client with this type of authentication is mostly suitable for working with custom Apex REST Services!
    //  Use SOAP API for any other tasks (e.g. CRUD operations with records, manipulating metadata, etc.)
    private static final RestApiClient CLIENT_BEARER_AUTH = new RestApiClient(
            usingApiKey("Authorization", "Bearer " + EnterpriseConnectionUtils.getInstance().getSessionId()),
            "Unable to get a response from Salesforce! Details: "
    );

    /**
     * Get Enterprise WSDL's XML file as a string.
     */
    public static String getEnterpriseWSDL() {
        var url = getSoapWsdlEnterpriseURL();
        return CLIENT_COOKIE_AUTH.get(url);
    }

    /**
     * Get Tooling WSDL's XML file as a string.
     */
    public static String getToolingWSDL() {
        var url = getSoapWsdlToolingURL();
        return CLIENT_COOKIE_AUTH.get(url);
    }

    /**
     * Get the current limits in the Salesforce org
     * (e.g. data storage, file storage, API request limits, etc.).
     */
    public static Limits getOrgCurrentLimits() {
        var url = getOrgLimitsURL();
        return CLIENT_BEARER_AUTH.get(url, Limits.class);
    }

    /**
     * Get a Country Code using a Country Name via the custom Apex REST API Service.
     * <br/>
     * For the reference see the records from the {@link CountryCodes__c} table.
     *
     * @param countryName any valid country name (e.g. "United States", "United Kingdom", "France", etc.)
     * @return country code as 3-symbol abbreviation (e.g. "USA" for "United States"),
     * or first 3 symbols of the country name if it's not found in {@link CountryCodes__c}.
     */
    public static String getCountryCodeFromSettingsService(String countryName) {
        var url = getSettingsServiceURL();
        var sfdcSettings = CLIENT_BEARER_AUTH.get(url);

        var countryCodes = new JSONObject(new JSONObject(sfdcSettings).getString("CountryCodes"));
        try {
            return countryCodes.getString(countryName.toLowerCase());
        } catch (JSONException e) {
            return countryName.toUpperCase().substring(0, 3);
        }
    }
}
