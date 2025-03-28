package com.aquiva.autotests.rc.utilities.salesforce;

import com.sforce.soap.metadata.*;
import com.sforce.ws.ConnectionException;
import io.qameta.allure.Step;

import java.util.*;

import static java.lang.String.format;

/**
 * Utility class that encapsulates the logic for working with SFDC using Metadata connection SOAP API.
 * The main usage for Metadata Connection is to create/delete records of Custom Metadata Type via API calls.
 * <br/><br/>
 * The class is designed using Singleton pattern
 * to be the "single point of contact" for users of Metadata connection API.
 */
public class MetadataConnectionUtils {

    /**
     * Special suffix in Full Names of the records of Custom Metadata Type.
     * Helps to find and delete automation test data from the sandboxes.
     */
    public static final String CRM_QA_AUTO_SUFFIX_FULLNAME = "CRM_QA_AUTO";

    //  Single instance of the class
    private static final MetadataConnectionUtils INSTANCE = new MetadataConnectionUtils();

    //  Connection object for accessing SFDC via SOAP API
    private final MetadataConnection metadataConnection;

    /**
     * Class constructor.
     * Contains the logic to establish an Enterprise connection with SFDC via SOAP API
     * and obtain a corresponding connection object.
     */
    private MetadataConnectionUtils() {
        try {
            metadataConnection = ConnectionFactory.getDefaultMetadataConnection();
        } catch (ConnectionException e) {
            throw new RuntimeException("Unable to create metadata connection! Details: " + e, e);
        }
    }

    /**
     * Get the instance of MetadataConnectionUtils object.
     * Useful for classes that contain Metadata API connection's operations logic.
     *
     * @return instance of MetadataConnectionUtils with the current session's data.
     */
    public static MetadataConnectionUtils getInstance() {
        return INSTANCE;
    }

    /**
     * Insert a record of the Custom Metadata Type (Custom MDT) in the SFDC org/sandbox's configuration.
     *
     * @param customMdtObjectName     Object name for Custom MDT
     *                                (see "Object Name" in SFDC UI, e.g. "Default_Business_Identity_Mapping")
     * @param customMdtRecordName     The unique Custom MDT record's name
     *                                <b>Note: the name must begin with a letter and use only alphanumeric characters and underscores.
     *                                The name cannot end with an underscore or have two consecutive underscores!</b>
     *                                (e.g. "CustomMdtRecordName1")
     * @param customMdtRecordLabel    Any label to distinguish a new record from others
     *                                (e.g. "Custom Mdt Record Name 1")
     * @param fieldNameToValueMapping mapping between field names and their values for Custom MDT
     *                                (e.g. {"FullName__c":"John Smith", "Language__c":"English"})
     * @return Full Name for the successfully created record of the Custom MDT
     * @throws ConnectionException in case of errors while accessing API
     */
    @Step
    public <T> String insertCustomMetadataTypeRecord(String customMdtObjectName,
                                                 String customMdtRecordName, String customMdtRecordLabel,
                                                     Map<String, T> fieldNameToValueMapping)
            throws ConnectionException {
        var customMetadata = new CustomMetadata();
        var customMdtRecordNameFormatted = format("%s_%s", customMdtRecordName, CRM_QA_AUTO_SUFFIX_FULLNAME);
        customMetadata.setFullName(format("%s.%s", customMdtObjectName, customMdtRecordNameFormatted));
        customMetadata.setLabel(format("%s (%s)", customMdtRecordLabel, CRM_QA_AUTO_SUFFIX_FULLNAME));

        setValuesToFieldsForCustomMetadata(customMetadata, fieldNameToValueMapping);

        var createdRecordsFullNames = SalesforceUtils.create(metadataConnection, customMetadata);
        return createdRecordsFullNames.get(0);
    }

    /**
     * Update a record of the Custom Metadata from the SFDC org/sandbox's configuration.
     *
     * @param customMetadata          the metadata that can be updated
     * @param fieldNameToValueMapping mapping between field names and their values for Custom MD
     *                                (e.g. {"FullName__c":"John Smith", "Language__c":"English"})
     * @return list of Full Names for the successfully updated records of the Custom Metadata
     * @throws ConnectionException in case of errors while accessing API
     */
    @Step
    public <T> List<String> updateCustomMetadataRecords(CustomMetadata customMetadata,
                                                        Map<String, T> fieldNameToValueMapping)
            throws ConnectionException {
        setValuesToFieldsForCustomMetadata(customMetadata, fieldNameToValueMapping);

        return SalesforceUtils.update(metadataConnection, customMetadata);
    }

    /**
     * Delete a record of the Custom Metadata Type (Custom MDT) from the SFDC org/sandbox's configuration.
     *
     * @param customMdtFullNames list of full names of the records to delete in the format
     *                           "CustomMdtObjectName.CustomMdtRecordName"
     *                           (e.g. ["Default_Business_Identity_Mapping.CustomMdtRecordName1",
     *                           "Default_Business_Identity_Mapping.CustomMdtRecordName2"])
     * @return list of Full Names for the successfully deleted records of the Custom MDT
     * @throws ConnectionException in case of errors while accessing API
     */
    @Step
    public List<String> deleteCustomMetadataRecords(List<String> customMdtFullNames) throws ConnectionException {
        return SalesforceUtils.delete(metadataConnection, "CustomMetadata", customMdtFullNames);
    }

    /**
     * Set Custom Metadata Values for the Custom Metadata object using the given 'field->value' mapping.
     *
     * @param customMetadata          any Custom Metadata object to set up with the values
     * @param fieldNameToValueMapping mapping between field names and their values for any Custom Metadata
     */
    private <T> void setValuesToFieldsForCustomMetadata(CustomMetadata customMetadata,
                                                        Map<String, T> fieldNameToValueMapping) {
        var fields = new ArrayList<CustomMetadataValue>();
        fieldNameToValueMapping.forEach((fieldName, fieldValue) -> {
            var field = new CustomMetadataValue();
            field.setField(fieldName);
            field.setValue(fieldValue);
            fields.add(field);
        });
        customMetadata.setValues(fields.toArray(new CustomMetadataValue[0]));
    }
}
