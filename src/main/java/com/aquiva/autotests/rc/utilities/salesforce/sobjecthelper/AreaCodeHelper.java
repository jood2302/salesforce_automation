package com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper;

import com.sforce.soap.enterprise.sobject.Area_Codes__c;

import static java.lang.Double.valueOf;

/**
 * Helper class to facilitate operations on {@link Area_Codes__c} objects.
 */
public class AreaCodeHelper extends SObjectHelper {
    //  Default address values
    private static final String DEFAULT_CITY = "Beverly Hills";
    private static final String DEFAULT_COUNTRY = "United States";
    private static final String DEFAULT_COUNTRY_CODE = "1";
    private static final String DEFAULT_STATE = "California";
    private static final String DEFAULT_STATE_ABBR = "CA";

    //  For 'Type__c' field
    private static final String TOLL_FREE_TYPE = "Toll-Free";
    private static final String LOCAL_TYPE = "Local";

    /**
     * Set area code's type depending on whether it's Toll-Free number or not.
     *
     * @param areaCode   Area_Codes__c object to set up with type
     * @param isTollFree true, if Area Code is Toll-Free (otherwise, it's Local type)
     */
    public static void setType(Area_Codes__c areaCode, boolean isTollFree) {
        areaCode.setType__c(isTollFree ? TOLL_FREE_TYPE : LOCAL_TYPE);
    }

    /**
     * Set default values to basic Area_Codes__c fields that may be useful in tests.
     *
     * @param areaCode Area_Codes__c instance to set up with default values
     */
    public static void setDefaultFields(Area_Codes__c areaCode) {
        areaCode.setCity__c(DEFAULT_CITY);
        areaCode.setCountry__c(DEFAULT_COUNTRY);
        areaCode.setCountry_Code__c(valueOf(DEFAULT_COUNTRY_CODE));
        areaCode.setState__c(DEFAULT_STATE);
        areaCode.setState_Abbreviation__c(DEFAULT_STATE_ABBR);
    }

    /**
     * Get composite full name from Area_Codes__c object.
     * Useful in some cases when user needs to choose value in area code full name format from UI.
     * <p></p>
     * <i>Example: San-Carlos Belmont, CA, United States (650)</i>
     *
     * @param areaCode Area_Codes__c object to get the name from
     * @return full name of Area Code
     */
    public static String getFullName(Area_Codes__c areaCode) {
        var result = new StringBuilder();

        result.append(areaCode.getCity__c()).append(", ");

        if (areaCode.getState_Abbreviation__c() != null) {
            result.append(areaCode.getState_Abbreviation__c()).append(", ");
        }

        result.append(areaCode.getCountry__c());
        result.append(" (").append(areaCode.getName()).append(")");

        return result.toString();
    }
}
