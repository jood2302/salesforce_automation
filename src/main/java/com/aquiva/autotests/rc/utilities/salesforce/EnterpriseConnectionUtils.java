package com.aquiva.autotests.rc.utilities.salesforce;

import com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ProcessWorkitemRequestHelper;
import com.sforce.soap.enterprise.*;
import com.sforce.soap.enterprise.sobject.ProcessInstanceWorkitem;
import com.sforce.soap.enterprise.sobject.SObject;
import com.sforce.ws.ConnectionException;
import io.qameta.allure.Step;

import java.util.*;
import java.util.stream.Stream;

import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.codeborne.selenide.Selenide.sleep;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

/**
 * Utility class that encapsulates the logic for working with SFDC using Enterprise connection SOAP API.
 * There are many useful methods: querying Salesforce's DB; performing DML operations with the data
 * (create, update, delete), etc...
 * <p></p>
 * The class is designed using Singleton pattern
 * to be the "single point of contact" for users of Enterprise connection API.
 */
public class EnterpriseConnectionUtils {

    //  Single instance of the class
    private static final EnterpriseConnectionUtils INSTANCE = new EnterpriseConnectionUtils();

    //  Connection object for accessing SFDC via SOAP API
    private final EnterpriseConnection enterpriseConnection;

    /**
     * Class constructor.
     * Contains the logic to establish an Enterprise connection with SFDC via SOAP API
     * and obtain a corresponding connection object.
     */
    private EnterpriseConnectionUtils() {
        try {
            enterpriseConnection = ConnectionFactory.getDefaultEnterpriseConnection();
        } catch (ConnectionException e) {
            throw new RuntimeException("Unable to create an Enterprise Connection! Details: " + e, e);
        }
    }

    /**
     * Get the instance of EnterpriseConnectionUtils object.
     * Useful for classes that contain Enterprise API connection's operations logic.
     *
     * @return instance of EnterpriseConnectionUtils with the current session's data.
     */
    public static EnterpriseConnectionUtils getInstance() {
        return INSTANCE;
    }

    /**
     * Get the Connection's session ID.
     * Useful in case of additional authorized access to the SFDC's content via other contexts
     * (e.g. web, REST API).
     */
    public String getSessionId() {
        return enterpriseConnection.getSessionHeader().getSessionId();
    }

    //  ### QUERY ###

    /**
     * Query the Salesforce database with the provided SOQL expression and
     * map the result to the provided SObject's type.
     *
     * <pre><code class='java'>
     * var connectionUtils = EnterpriseConnectionUtils.getInstance();
     * var contacts = connectionUtils.query(
     *         "SELECT Id, FirstName, LastName " +
     *         "FROM Contact " +
     *         "WHERE Email != null",
     *     Contact.class);
     * </code>
     *
     * Then, these 'contacts' might look like this:
     * - Contact{Id='0031k00000bhPQeAAM', FirstName='James', LastName='Bond'}
     * - Contact{Id='0031k00000bWaLcAAK', FirstName='Maria', LastName='Poppins'}
     * - etc...
     * </pre>
     *
     * <b> Note: all resulting objects will only contain values in the queried fields ("SELECT" part)!
     * Non-null (in the DB) fields that weren't queried will remain null on the Java object!
     * </b>
     *
     * @param queryString SOQL expression that queries one or several fields for any SObject
     *                    (e.g. <i>"SELECT Id, FirstName, LastName FROM Contact WHERE Email != null"</i>)
     * @param valueType   any valid standard or custom SObject type
     *                    (e.g. Account, Contact, Opportunity...)
     * @return list of SObjects that were found using the provided query
     * @throws ConnectionException in case of errors while accessing API
     */
    @Step
    public <T extends SObject> List<T> query(String queryString, Class<T> valueType) throws ConnectionException {
        var records = transactionWithRetries(() -> enterpriseConnection.query(queryString).getRecords());

        return Arrays.stream(records)
                .map(valueType::cast)
                .collect(toList());
    }

    /**
     * Query the Salesforce database with the provided SOQL expression and
     * map the result to the provided SObject's type.
     * <br/>
     * Note: the method returns only the single object!
     *
     * @param queryString SOQL expression that queries one or several fields for any SObject
     *                    (e.g. <i>"SELECT Id, FirstName, LastName FROM Contact WHERE Email != null LIMIT 1"</i>)
     * @param valueType   any valid standard or custom SObject type
     *                    (e.g. Account, Contact, Opportunity...)
     * @return single SObject that was found using the provided query
     * @throws ConnectionException in case of errors while accessing API
     * @throws AssertionError      if query returns more or less than 1 record
     * @see EnterpriseConnectionUtils#query(String, Class)
     */
    @Step
    public <T extends SObject> T querySingleRecord(String queryString, Class<T> valueType) throws ConnectionException {
        var resultList = query(queryString, valueType);

        if (resultList.size() != 1) {
            throw new AssertionError(format("Query supposed to return 1 record, but returned %d element(s). \n Query: %s",
                    resultList.size(), queryString));
        } else {
            return resultList.get(0);
        }
    }

    //  ### CREATE ###

    /**
     * Insert the provided SObject(-s) into the Salesforce DB and return the list of its/their resulting ID(-s).
     * <p></p>
     * This method also assigns resulting IDs to their corresponding provided SObjects.
     *
     * @param objects array of SObjects (or a single SObject) to insert into the database
     * @return list of the IDs for all provided SObjects after inserting them into the database
     * @throws ConnectionException in case of errors while accessing API
     */
    @Step
    public List<String> insertAndGetIds(SObject... objects) throws ConnectionException {
        var insertResult = transactionWithRetries(() -> SalesforceUtils.create(enterpriseConnection, objects));

        for (int i = 0; i < insertResult.size(); i++) {
            objects[i].setId(insertResult.get(i));
        }

        return insertResult;
    }

    /**
     * Insert the provided SObjects into the Salesforce DB and return the list of their resulting IDs.
     * <p></p>
     * This method also assigns resulting IDs to their corresponding provided SObjects.
     *
     * @param objects list of SObjects to insert into the database
     * @return list of the IDs for all provided SObjects after inserting them into the database
     * @throws ConnectionException in case of errors while accessing API
     */
    @Step
    public List<String> insertAndGetIds(List<? extends SObject> objects) throws ConnectionException {
        return insertAndGetIds(objects.toArray(new SObject[0]));
    }

    //  ### UPDATE ###

    /**
     * Update the provided SObjects in the Salesforce database.
     * <p></p>
     * Note: all SObjects should contain non-null corresponding ID (SObject.Id field),
     * i.e. exist in the database.
     * Of course, they should also carry the updated state
     * (e.g. new values on their fields).
     *
     * @param objects SObjects that needs to be updated.
     * @return list of SObjects' IDs that were successfully updated
     * @throws ConnectionException in case of errors while accessing API
     */
    @Step
    public List<String> update(SObject... objects) throws ConnectionException {
        return transactionWithRetries(() -> SalesforceUtils.update(enterpriseConnection, objects));
    }

    /**
     * Update the provided SObjects in the Salesforce database.
     * <p></p>
     * Note: all SObjects should contain non-null corresponding ID (SObject.Id field),
     * i.e. exist in the database.
     * Of course, they should also carry the updated state
     * (e.g. new values on their fields).
     *
     * @param objects list of SObjects that need to be updated
     * @return list of SObjects' IDs that were successfully updated
     * @throws ConnectionException in case of errors while accessing API
     */
    @Step
    public List<String> update(Collection<? extends SObject> objects) throws ConnectionException {
        return update(objects.toArray(new SObject[0]));
    }

    //  ### DELETE ###

    /**
     * Delete the provided SObject(-s) from the database.
     *
     * @param objects array of SObjects (or just one) that need to be deleted
     * @return list of SObjects' IDs that were successfully deleted
     * @throws ConnectionException in case of errors while accessing API
     */
    @Step
    public List<String> delete(SObject... objects) throws ConnectionException {
        var ids = Arrays.stream(objects)
                .map(SObject::getId)
                .toArray(String[]::new);

        return deleteByIds(ids);
    }

    /**
     * Delete the provided SObjects from the database.
     *
     * @param objects collection of SObjects that need to be deleted
     * @return list of SObjects' IDs that were successfully deleted
     * @throws ConnectionException in case of errors while accessing API
     */
    @Step
    public <T extends SObject> List<String> delete(Collection<T> objects) throws ConnectionException {
        var ids = objects.stream()
                .map(SObject::getId)
                .toArray(String[]::new);

        return deleteByIds(ids);
    }

    /**
     * Delete SObjects with the provided IDs from the database.
     *
     * @param ids IDs for SObjects that need to be deleted
     * @return list of SObjects' IDs that were successfully deleted
     * @throws ConnectionException in case of errors while accessing API
     */
    @Step
    public List<String> deleteByIds(Collection<String> ids) throws ConnectionException {
        return deleteByIds(ids.toArray(new String[0]));
    }

    /**
     * Delete SObjects with the provided IDs from the database.
     *
     * @param ids IDs for SObjects that need to be deleted
     * @return list of SObjects' IDs that were successfully deleted
     * @throws ConnectionException in case of errors while accessing API
     */
    @Step
    public List<String> deleteByIds(String... ids) throws ConnectionException {
        return SalesforceUtils.delete(enterpriseConnection, ids);
    }

    //  ### ADDITIONAL METHODS ###

    /**
     * Get ID for a Record Type of the given SObject.
     * Useful when creating non-standard SObjects via API.
     *
     * @param sObjectName    API name for the given SObject
     *                       (e.g. "Account", "Contact", "Opportunity", "Approval__c"...)
     * @param recordTypeName name for the SObject's record type
     *                       (e.g. "Partner Leads" for Lead, "Invoicing Request" for Approval__c ...).
     *                       Totally depends on the actual org's configuration.
     * @return ID for the SObject's record type
     * @throws ConnectionException in case of errors while accessing API
     */
    public String getRecordTypeId(String sObjectName, String recordTypeName) throws ConnectionException {
        var describeSObjectResult = enterpriseConnection.describeSObject(sObjectName);
        var recordTypeInfo = Arrays.stream(describeSObjectResult.getRecordTypeInfos())
                .filter(s -> s.getName().equals(recordTypeName))
                .findAny();

        return recordTypeInfo.isPresent() ?
                recordTypeInfo.get().getRecordTypeId() :
                EMPTY_STRING;
    }

    /**
     * Submit a single record for approval via standard Salesforce Approval Process.
     *
     * @param sObjectId ID of the SFDC record to be submitted for approval
     */
    @Step
    public void submitRecordForApproval(String sObjectId) {
        var processSubmitRequest = new ProcessSubmitRequest();
        processSubmitRequest.setObjectId(sObjectId);
        processSubmitRequest.setComments("Submitted for Approval by QA Automation SOAP API Call");

        transactionWithRetries(() -> SalesforceUtils.sendApprovalProcessInstancesAction(enterpriseConnection, processSubmitRequest));
    }

    /**
     * Approve a single record via standard Salesforce Approval Process.
     * It sends "Approve" action for the submitted approval process.
     *
     * @param sObjectId ID of the SFDC record to be approved
     * @throws ConnectionException in case of errors while accessing API
     */
    @Step
    public void approveSingleRecord(String sObjectId) throws ConnectionException {
        var piWorkItems = query(
                "SELECT Id " +
                        "FROM ProcessInstanceWorkitem " +
                        "WHERE ProcessInstance.TargetObjectId = '" + sObjectId + "'",
                ProcessInstanceWorkitem.class);

        var approvalRequests = piWorkItems.stream()
                .map(processInstanceWorkitem -> {
                    var pWorkItemRequest = new ProcessWorkitemRequest();
                    ProcessWorkitemRequestHelper.setFieldsForApproveAction(pWorkItemRequest, processInstanceWorkitem.getId());
                    return pWorkItemRequest;
                })
                .toArray(ProcessWorkitemRequest[]::new);

        transactionWithRetries(() -> SalesforceUtils.sendApprovalProcessInstancesAction(enterpriseConnection, approvalRequests));
    }

    /**
     * Reject a single record via standard Salesforce Approval Process.
     * It sends "Reject" action for the submitted approval process.
     *
     * @param sObjectId ID of the SFDC record to be rejected
     * @throws ConnectionException in case of errors while accessing API
     */
    @Step
    public void rejectSingleRecord(String sObjectId) throws ConnectionException {
        var piWorkItems = query(
                "SELECT Id " +
                        "FROM ProcessInstanceWorkitem " +
                        "WHERE ProcessInstance.TargetObjectId = '" + sObjectId + "'",
                ProcessInstanceWorkitem.class);

        var approvalRequests = piWorkItems.stream()
                .map(piWorkItem -> {
                    var pWorkItemRequest = new ProcessWorkitemRequest();
                    ProcessWorkitemRequestHelper.setFieldsForRejectAction(pWorkItemRequest, piWorkItem.getId());
                    return pWorkItemRequest;
                })
                .toArray(ProcessWorkitemRequest[]::new);

        transactionWithRetries(() -> SalesforceUtils.sendApprovalProcessInstancesAction(enterpriseConnection, approvalRequests));
    }

    /**
     * Run SFDC Enterprise API operation,
     * and retry several times, if it fails with one of the exceptions eligible for the retry.
     *
     * @param operation any API operation like create/update/delete record(s).
     * @return result of the API operation (e.g. list of the inserted Salesforce IDs
     * after insert/create operation)
     * @throws RuntimeException in case if number of retries are maxed out,
     *                          or different exception is thrown from the API
     */
    private <T> T transactionWithRetries(RunnableWithException<T> operation) {
        var retries = parseInt(System.getProperty("apiTransactionRetries", "3"));
        while (true) {
            try {
                return operation.run();
            } catch (Exception exception) {
                retries--;
                var exceptionDetails = exception.getMessage() != null
                        ? exception.getMessage()
                        : exception.toString();
                var isExceptionForRetry = Stream.of("UNABLE_TO_LOCK_ROW", "Failed to send request to",
                                "ConnectionTimeout", "An unexpected error occurred", 
                                "QUERY_TIMEOUT", "Your query request was running for too long")
                        .anyMatch(exceptionForRetry -> exceptionDetails.contains(exceptionForRetry));
                if (isExceptionForRetry && retries >= 0) {
                    sleep(5_000L);
                } else {
                    throw new RuntimeException(exceptionDetails, exception);
                }
            }
        }
    }

    /**
     * Additional interface to wrap API transactions with SFDC.
     * Helps to handle some exceptions in transactions (e.g. "UNABLE_TO_LOCK_ROW").
     */
    @FunctionalInterface
    private interface RunnableWithException<T> {
        T run() throws RuntimeException, ConnectionException;
    }
}
