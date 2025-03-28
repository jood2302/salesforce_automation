package com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories;

import com.sforce.soap.enterprise.sobject.Default_Business_Identity_Mapping__mdt;
import com.sforce.ws.ConnectionException;

import java.util.Map;

import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.aquiva.autotests.rc.utilities.StringHelper.getRandomPositiveInteger;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.DefaultBusinessIdentityMappingHelper.DEFAULT_BUSINESS_IDENTITY_MAPPING_OBJECT_NAME;

/**
 * Factory class for creating quick instances of {@link Default_Business_Identity_Mapping__mdt}
 * custom metadata type.
 * <br/>
 * All factory methods also insert created custom metadata type records into the SF database.
 */
public class DefaultBusinessIdentityMappingFactory extends SObjectFactory {

    /**
     * Create a new Default Business Identity Mapping custom metadata type record
     * with only Default Business Identity field populated
     * and insert it into Salesforce via Metadata API.
     *
     * @param label                   label of the Default Business Identity Mapping record
     *                                (e.g "CA RingCentral Canada")
     * @param subBrand                sub-brand of the Default Business Identity Mapping record
     *                                (e.g. "Test SubBrand 123456")
     * @param brandName               name of the brand of the Default Business Identity Mapping record
     *                                (e.g. "RingCentral Canada")
     * @param country                 country of the Default Business Identity Mapping record
     *                                (e.g. "Canada")
     * @param defaultBusinessIdentity default business identity value of the Default Business Identity Mapping record
     *                                (e.g. "RingCentral Canada")
     * @return a Full Name (as in Metadata) of the new Default Business Identity Mapping custom metadata type record
     * @throws ConnectionException in case of errors while accessing API
     */
    public static String createDefaultBusinessIdentityMapping(String label, String subBrand,
                                                              String brandName, String country,
                                                              String defaultBusinessIdentity)
            throws ConnectionException {
        return createDefaultBusinessIdentityMapping(label, subBrand, brandName, country, defaultBusinessIdentity, EMPTY_STRING);
    }

    /**
     * Create a new Default Business Identity Mapping custom metadata type record
     * with Default Business Identity and Available Business Identities populated
     * and with the 'Core Brand' field set to false (by default)
     * and insert it into Salesforce via Metadata API.
     *
     * @param label                       label of the Default Business Identity Mapping record
     *                                    (e.g "CA RingCentral Canada")
     * @param subBrand                    sub-brand of the Default Business Identity Mapping record
     *                                    (e.g. "Test SubBrand 123456")
     * @param brandName                   name of the brand of the Default Business Identity Mapping record
     *                                    (e.g. "RingCentral Canada")
     * @param country                     country of the Default Business Identity Mapping record
     *                                    (e.g. "Canada")
     * @param defaultBusinessIdentity     default business identity value of the Default Business Identity Mapping record
     *                                    (e.g. "RingCentral Canada")
     * @param availableBusinessIdentities list of available business identities of the Default Business Identity Mapping record
     *                                    (e.g. "RingCentral Ltd(France);RingCentral CH GmbH")
     * @return a Full Name (as in Metadata) of the new Default Business Identity Mapping custom metadata type record
     * @throws ConnectionException in case of errors while accessing API
     */
    public static String createDefaultBusinessIdentityMapping(String label, String subBrand,
                                                              String brandName, String country,
                                                              String defaultBusinessIdentity,
                                                              String availableBusinessIdentities)
            throws ConnectionException {
        return createDefaultBusinessIdentityMapping(label, subBrand, brandName, country,
                defaultBusinessIdentity, availableBusinessIdentities, false);
    }

    /**
     * Create a new Default Business Identity Mapping custom metadata type record
     * with Default Business Identity and Available Business Identities populated.
     *
     * @param label                       label of the Default Business Identity Mapping record
     *                                    (e.g "CA RingCentral Canada")
     * @param subBrand                    sub-brand of the Default Business Identity Mapping record
     *                                    (e.g. "Test SubBrand 123456")
     * @param brandName                   name of the brand of the Default Business Identity Mapping record
     *                                    (e.g. "RingCentral Canada")
     * @param country                     country of the Default Business Identity Mapping record
     *                                    (e.g. "Canada")
     * @param defaultBusinessIdentity     default business identity value of the Default Business Identity Mapping record
     *                                    (e.g. "RingCentral Canada")
     * @param availableBusinessIdentities list of available business identities of the Default Business Identity Mapping record
     *                                    (e.g. "RingCentral Ltd(France);RingCentral CH GmbH")
     * @param isCoreBrand                 whether the brand is a core brand or not
     * @return a Full Name (as in Metadata) of the new Default Business Identity Mapping custom metadata type record
     * @throws ConnectionException in case of errors while accessing API
     */
    public static String createDefaultBusinessIdentityMapping(String label, String subBrand,
                                                              String brandName, String country,
                                                              String defaultBusinessIdentity,
                                                              String availableBusinessIdentities,
                                                              boolean isCoreBrand)
            throws ConnectionException {
        var fields = Map.of(
                "Brand__c", brandName,
                "Country__c", country,
                "Default_Business_Identity__c", defaultBusinessIdentity,
                "Sub_Brand__c", subBrand,
                "Available_Business_Identities__c", availableBusinessIdentities,
                "Is_Core_Brand__c", isCoreBrand
        );
        return METADATA_CONNECTION_UTILS.insertCustomMetadataTypeRecord(
                DEFAULT_BUSINESS_IDENTITY_MAPPING_OBJECT_NAME,
                "DefaultBiMapping" + getRandomPositiveInteger(), label, fields
        );
    }
}
