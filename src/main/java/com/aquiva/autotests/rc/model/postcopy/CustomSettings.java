package com.aquiva.autotests.rc.model.postcopy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sforce.soap.enterprise.sobject.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * An object that stores values for setting in Custom Settings.
 */
public class CustomSettings {
    @JsonProperty("GW_Settings__c")
    public GWSettings gwSettings;
    @JsonProperty("EndpointsSettings__c")
    public EndpointsSettings endpointsSettings;
    @JsonProperty("NGBS_Settings__c")
    public NgbsSettings ngbsSettings;
    @JsonProperty("CCB_Settings__c")
    public CcbSettings ccbSettings;

    /**
     * Collect the necessary data for Remote Site Settings in a format that stores SiteName as a key
     * and EndpointUrl as a value.
     *
     * @return data for creating/checking of Remote Site Settings
     */
    public Map<String, String> collectRemoteSiteSettings() {
        return Map.of(
                "UQT_Server_Endpoint", this.getEndpointUrl(this.endpointsSettings.uqtServerEndpoint),
                "GW_Endpoint", this.getEndpointUrl(this.gwSettings.baseEndpoint),
                "NGBS_Base_Endpoint", this.getEndpointUrl(this.ngbsSettings.baseEndpoint),
                "CCB_Base_Endpoint", this.getEndpointUrl(this.ccbSettings.baseEndpoint)
        );
    }

    /**
     * Format and return Endpoint URL in needed format(scheme and authority) from provided URL.
     *
     * @param url URL that need to be formatted
     * @return formatted Endpoint URL(scheme and authority, e.g. 'http://www.example.com:80')
     */
    public String getEndpointUrl(String url) {
        try {
            var endpointUrl = new URL(url);
            return endpointUrl.getProtocol() + "://" + endpointUrl.getAuthority();
        } catch (MalformedURLException e) {
            throw new RuntimeException("URL has wrong format! Details: " + e, e);
        }
    }

    /**
     * An object that stores values for {@link GW_Settings__c} Custom Setting.
     */
    public static class GWSettings {
        @JsonProperty("BaseEndpoint__c")
        public String baseEndpoint;
    }

    /**
     * An object that stores values for {@link EndpointsSettings__c} Custom Setting.
     */
    public static class EndpointsSettings {
        @JsonProperty("UQTServerEndpoint__c")
        public String uqtServerEndpoint;
    }

    /**
     * An object that stores values for {@link NGBS_Settings__c} Custom Setting.
     */
    public static class NgbsSettings {
        @JsonProperty("BaseEndpoint__c")
        public String baseEndpoint;
        @JsonProperty("Login__c")
        public String login;
        @JsonProperty("Password__c")
        public String password;
        @JsonProperty("AccountManagerPath__c")
        public String accountManagerPath;
        @JsonProperty("InvoiceEnginePath__c")
        public String invoiceEnginePath;
        @JsonProperty("CatalogPath__c")
        public String catalogPath;
        @JsonProperty("CostCenterNestingLevel__c")
        public double costCenterNestingLevel;
    }

    /**
     * An object that stores values for {@link CCB_Settings__c} Custom Setting.
     */
    public static class CcbSettings {
        @JsonProperty("BaseEndpoint__c")
        public String baseEndpoint;
        @JsonProperty("Login__c")
        public String login;
        @JsonProperty("Password__c")
        public String password;
        @JsonProperty("SignUpPath__c")
        public String signUpPath;
        @JsonProperty("TerminationPath__c")
        public String terminationPath;
    }
}
