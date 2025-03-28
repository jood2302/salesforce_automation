package com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper;

import com.sforce.soap.enterprise.sobject.Task;
import com.sforce.ws.ConnectionException;

/**
 * Helper class to facilitate operations on {@link Task} objects.
 */
public class TaskHelper extends SObjectHelper {
    //  SFDC API parameters
    private static final String S_OBJECT_API_NAME = "Task";
    private static final String STANDARD_TASK_RECORD_TYPE = "Standard Task";

    /**
     * Set 'Standard Task' record type for the Task object.
     *
     * @param task Task object to set up Record type on
     * @throws ConnectionException in case of errors while accessing API
     */
    public static void setStandardTaskRecordType(Task task) throws ConnectionException {
        var standardTaskRecordTypeId = CONNECTION_UTILS.getRecordTypeId(S_OBJECT_API_NAME, STANDARD_TASK_RECORD_TYPE);
        task.setRecordTypeId(standardTaskRecordTypeId);
    }
}
