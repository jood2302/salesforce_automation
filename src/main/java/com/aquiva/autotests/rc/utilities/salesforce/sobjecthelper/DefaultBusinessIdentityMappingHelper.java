package com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper;

import com.sforce.soap.enterprise.sobject.Default_Business_Identity_Mapping__mdt;
import com.sforce.soap.metadata.CustomMetadata;

import static java.lang.String.format;

/**
 * Helper class to facilitate operations on {@link Default_Business_Identity_Mapping__mdt} objects.
 */
public class DefaultBusinessIdentityMappingHelper extends SObjectHelper {

    public static final String DEFAULT_BUSINESS_IDENTITY_MAPPING_OBJECT_NAME = "Default_Business_Identity_Mapping";

    /**
     * Get the Full Name for the Metadata API of the given {@link Default_Business_Identity_Mapping__mdt} record.
     *
     * @param defaultBiMapping a created {@link Default_Business_Identity_Mapping__mdt} record
     *                         with the non-null {@code DeveloperName}
     * @return a Full Name (as in Metadata) of the new Default Business Identity Mapping custom metadata type record
     * (e.g. "Default_Business_Identity_Mapping.DefaultBiMapping12345")
     */
    public static String getFullName(Default_Business_Identity_Mapping__mdt defaultBiMapping) {
        return format("%s.%s", DEFAULT_BUSINESS_IDENTITY_MAPPING_OBJECT_NAME, defaultBiMapping.getDeveloperName());
    }

    /**
     * Return a Custom Metadata object for the given Default Business Identity Mapping custom metadata type record.
     * Should be used in the 'update' DML operation alongside a mapping of the updated fields->values.
     *
     * @param defaultBiMapping a created {@link Default_Business_Identity_Mapping__mdt} record
     *                         with the non-null {@code DeveloperName} and {@code MasterLabel} fields.
     */
    public static CustomMetadata getCustomMetadataToUpdateDefaultBiMapping(Default_Business_Identity_Mapping__mdt defaultBiMapping) {
        var customMetadata = new CustomMetadata();
        customMetadata.setFullName(getFullName(defaultBiMapping));
        customMetadata.setLabel(defaultBiMapping.getMasterLabel());
        return customMetadata;
    }
}
