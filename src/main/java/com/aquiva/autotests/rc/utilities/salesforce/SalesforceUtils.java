package com.aquiva.autotests.rc.utilities.salesforce;

import com.sforce.soap.enterprise.*;
import com.sforce.soap.enterprise.sobject.SObject;
import com.sforce.soap.metadata.*;
import com.sforce.soap.tooling.ToolingConnection;
import com.sforce.ws.ConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static io.qameta.allure.Allure.step;
import static java.util.stream.Collectors.toList;

/**
 * Utility class providing additional processing for Salesforce API operations.
 * Such operations might need some additional logging and exception handling.
 * <p></p>
 * For example, if we update some SObjects, and operation partially fails,
 * then it's good to know what errors happened, and what SObjects were problematic.
 * This information might be useful for debugging, and the aftermath "housekeeping" activities.
 */
public class SalesforceUtils {
    private static final Logger LOG = LoggerFactory.getLogger(SalesforceUtils.class);

    /**
     * Create new enterprise SObjects in the Salesforce database
     * (any standard/custom objects that exist in the Enterprise WSDL).
     *
     * @param enterpriseConnection Enterprise connection with Salesforce via SOAP API
     * @param objects              enterprise SObjects to be created in the database
     * @return list of SObject IDs for every created SObject in the database
     * @throws ConnectionException in case of errors while accessing API
     */
    public static List<String> create(EnterpriseConnection enterpriseConnection,
                                      SObject... objects)
            throws ConnectionException {
        var saveResults = enterpriseConnection.create(objects);
        var successIdList = new ArrayList<String>();
        var errorMessages = new ArrayList<String>();

        for (var saveResult : saveResults) {
            if (saveResult.getSuccess()) {
                successIdList.add(saveResult.getId());
            } else {
                var errors = saveResult.getErrors();

                if (errors.length != 0) {
                    for (var error : errors) {
                        errorMessages.add(error.toString());
                    }
                }
            }
        }

        if (!successIdList.isEmpty()) {
            step("Successfully created SObject(s) with IDs: " + successIdList);
        }

        if (!errorMessages.isEmpty()) {
            throw new RuntimeException("Failed to insert SObject(s)! \n" +
                    "Errors: " + errorMessages);
        }

        return successIdList;
    }

    /**
     * Create new tooling SObjects in the Salesforce database
     * (any standard objects and metadata that exist in the Tooling WSDL).
     *
     * @param toolingConnection Tooling connection with Salesforce via SOAP API
     * @param objects           tooling SObjects to be created in the database
     * @return list of SObject IDs for every created SObject in the database
     * @throws ConnectionException in case of errors while accessing API
     */
    public static List<String> create(ToolingConnection toolingConnection,
                                      com.sforce.soap.tooling.sobject.SObject... objects)
            throws ConnectionException {
        var saveResults = toolingConnection.create(objects);
        var successIdList = new ArrayList<String>();
        var errorMessages = new ArrayList<String>();

        for (var saveResult : saveResults) {
            if (saveResult.getSuccess()) {
                successIdList.add(saveResult.getId());
            } else {
                var errors = saveResult.getErrors();

                if (errors.length != 0) {
                    for (var error : errors) {
                        errorMessages.add(error.toString());
                    }
                }
            }
        }

        if (!successIdList.isEmpty()) {
            step("Successfully created SObject(s) with ID(s): " + successIdList);
        }

        if (!errorMessages.isEmpty()) {
            throw new RuntimeException("Failed to insert SObject(s)! \n" +
                    "Errors: " + errorMessages);
        }

        return successIdList;
    }

    /**
     * Create new Metadata in the Salesforce's org/sandbox configuration
     * (any metadata that could be created via in the Metadata WSDL).
     *
     * @param metadataConnection Metadata connection with Salesforce via SOAP API
     * @param customMetadata     any metadata that can be created (e.g. records of the Custom Metadata Type)
     * @return list of successfully created Custom Metadata Full Names' entities
     * @throws ConnectionException in case of errors while accessing API
     */
    public static List<String> create(MetadataConnection metadataConnection, CustomMetadata customMetadata)
            throws ConnectionException {
        var saveResults = metadataConnection.createMetadata(new Metadata[]{customMetadata});
        var successFullNamesList = new ArrayList<String>();
        var errorMessages = new ArrayList<String>();

        for (var saveResult : saveResults) {
            if (saveResult.getSuccess()) {
                successFullNamesList.add(saveResult.getFullName());
            } else {
                var errors = saveResult.getErrors();

                if (errors.length != 0) {
                    for (var error : errors) {
                        errorMessages.add(error.toString());
                    }
                }
            }
        }

        if (!successFullNamesList.isEmpty()) {
            step("Successfully created metadata with FullName(s): " + successFullNamesList);
        }

        if (!errorMessages.isEmpty()) {
            throw new RuntimeException("Failed to create Metadata! \n" +
                    "Errors: " + errorMessages);
        }

        return successFullNamesList;
    }

    /**
     * Update the state of the provided SObjects in the Salesforce database.
     *
     * @param enterpriseConnection Enterprise connection with Salesforce via SOAP API
     * @param objects              SObjects to be updated in the database
     * @return list of SObject IDs for every updated SObject in the database
     * @throws ConnectionException in case of errors while accessing API
     */
    public static List<String> update(EnterpriseConnection enterpriseConnection, SObject... objects)
            throws ConnectionException {
        var saveResults = enterpriseConnection.update(objects);
        var successIdList = new ArrayList<String>();
        var errorMessages = new ArrayList<String>();

        for (var saveResult : saveResults) {
            if (saveResult.getSuccess()) {
                successIdList.add(saveResult.getId());
            } else {
                var errors = saveResult.getErrors();

                if (errors.length != 0) {
                    for (var error : errors) {
                        errorMessages.add(error.toString());
                    }
                }
            }
        }

        if (!errorMessages.isEmpty()) {
            var failedIds = Arrays.stream(objects)
                    .map(SObject::getId)
                    .collect(toList());
            failedIds.removeAll(successIdList);

            throw new RuntimeException("Failed to update SObject(s) with id(s): " + failedIds + "\n" +
                    "Errors: " + errorMessages);
        }

        return successIdList;
    }

    /**
     * Update a given Metadata in the Salesforce's org/sandbox configuration.
     *
     * @param metadataConnection Metadata connection with Salesforce via SOAP API
     * @param customMetadata     any metadata that can be updated (e.g. records of the Custom Metadata Type)
     * @return list of successfully updated Custom Metadata Full Names' entities
     * @throws ConnectionException in case of errors while accessing API
     */
    public static List<String> update(MetadataConnection metadataConnection,
                                      CustomMetadata customMetadata) throws ConnectionException {
        var saveResults = metadataConnection.updateMetadata(new Metadata[]{customMetadata});
        var successFullNamesList = new ArrayList<String>();
        var errorMessages = new ArrayList<String>();

        for (var saveResult : saveResults) {
            if (saveResult.getSuccess()) {
                successFullNamesList.add(saveResult.getFullName());
            } else {
                var errors = saveResult.getErrors();

                if (errors.length != 0) {
                    for (var error : errors) {
                        errorMessages.add(error.toString());
                    }
                }
            }
        }

        if (!successFullNamesList.isEmpty()) {
            step("Successfully updated metadata with FullName(s): " + successFullNamesList);
        }

        if (!errorMessages.isEmpty()) {
            throw new RuntimeException("Failed to update Metadata! \n" +
                    "Errors: " + errorMessages);
        }

        return successFullNamesList;
    }

    /**
     * Delete SObjects with the provided IDs from the Salesforce database.
     *
     * @param enterpriseConnection Enterprise connection with Salesforce via SOAP API
     * @param ids                  SObjects' IDs to be deleted in the database
     * @return list of SObject IDs for every deleted SObject in the database
     * @throws ConnectionException in case of errors while accessing API
     */
    public static List<String> delete(EnterpriseConnection enterpriseConnection, String... ids)
            throws ConnectionException {
        var deleteResults = enterpriseConnection.delete(ids);
        var successIdList = new ArrayList<String>();
        var errorMessages = new ArrayList<String>();

        for (var deleteResult : deleteResults) {
            if (deleteResult.getSuccess()) {
                successIdList.add(deleteResult.getId());
            } else {
                var errors = deleteResult.getErrors();

                if (errors.length != 0) {
                    for (var error : errors) {
                        if (error.toString().contains("ENTITY_IS_DELETED")) {
                            LOG.warn("Entity with ID {} is already deleted! Details: {}", deleteResult.getId(), error);
                            successIdList.add(deleteResult.getId());
                        } else {
                            errorMessages.add(error.toString());
                        }
                    }
                }
            }
        }

        if (!errorMessages.isEmpty()) {
            var failedIds = new ArrayList<>(Arrays.asList(ids));
            failedIds.removeAll(successIdList);

            throw new RuntimeException("Failed to delete SObject(s) with id(s): " + failedIds + "\n" +
                    "Errors: " + errorMessages);
        }

        return successIdList;
    }

    /**
     * Delete Metadata with the provided type and fully qualified names
     * from the current Salesforce org/sandbox configuration.
     *
     * @param metadataConnection Metadata connection with Salesforce via SOAP API
     * @param metadataType       any valid metadata type to delete (e.g. "CustomMetadata")
     * @param metadataFullNames  fully qualified names for metadata to delete (e.g. "Custom_MDT_Name.RecordName123")
     * @return list of successfully deleted Custom Metadata Full Names' entities
     * @throws ConnectionException in case of errors while accessing API
     */
    public static List<String> delete(MetadataConnection metadataConnection,
                                      String metadataType, List<String> metadataFullNames)
            throws ConnectionException {
        var deleteResults = metadataConnection.deleteMetadata(metadataType, metadataFullNames.toArray(new String[0]));
        var successFullNamesList = new ArrayList<String>();
        var errorMessages = new ArrayList<String>();

        for (var deleteResult : deleteResults) {
            if (deleteResult.getSuccess()) {
                successFullNamesList.add(deleteResult.getFullName());
            } else {
                var errors = deleteResult.getErrors();

                if (errors.length != 0) {
                    for (var error : errors) {
                        errorMessages.add(error.toString());
                    }
                }
            }
        }

        if (!errorMessages.isEmpty()) {
            var failedFullNames = new ArrayList<>(metadataFullNames);
            failedFullNames.removeAll(successFullNamesList);

            throw new RuntimeException("Failed to delete Metadata with Full Name(s): " + failedFullNames + "\n" +
                    "Errors: " + errorMessages);
        }

        return successFullNamesList;
    }

    /**
     * Send instances of the standard Approval Process to be submittied for approval, approved, or rejected.
     * Basically, it submits for approval/approves/rejects SFDC records related to these instances.
     *
     * @param enterpriseConnection Enterprise connection with Salesforce via SOAP API
     * @param processRequests      special approval action requests that address the objects
     *                             that needs to be or already have been submitted to the approval process
     * @return any results of the approval action from the SFDC
     * @throws ConnectionException in case of errors while accessing API
     */
    public static ProcessResult[] sendApprovalProcessInstancesAction(EnterpriseConnection enterpriseConnection,
                                                                     ProcessRequest... processRequests)
            throws ConnectionException {
        var processResults = enterpriseConnection.process(processRequests);

        var processResultsErrors = new ArrayList<String>();
        for (var processResult : processResults) {
            if (!processResult.getSuccess()) {
                for (var error : processResult.getErrors())
                    processResultsErrors.add(error.toString());
            }
        }

        if (!processResultsErrors.isEmpty()) {
            throw new RuntimeException("Errors occurred while submitting for approval/approving/rejecting records! \n" +
                    "Errors: " + processResultsErrors);
        }

        return processResults;
    }
}
