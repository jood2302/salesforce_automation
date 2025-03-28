package com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper;

import com.sforce.soap.enterprise.ProcessWorkitemRequest;

/**
 * Helper class to facilitate operations on {@link ProcessWorkitemRequest} objects.
 */
public class ProcessWorkitemRequestHelper extends SObjectHelper {

    //  For 'Action' field
    public static final String APPROVE_ACTION = "Approve";
    public static final String REJECT_ACTION = "Reject";

    /**
     * Set default values for approval process work item request to be sent via API
     * to approve SFDC record.
     *
     * @param request      ProcessWorkitemRequest to set up
     * @param piWorkItemId ID of the ProcessInstanceWorkitem record that's related to SFDC record to be approved
     */
    public static void setFieldsForApproveAction(ProcessWorkitemRequest request, String piWorkItemId) {
        request.setWorkitemId(piWorkItemId);
        request.setAction(APPROVE_ACTION);
        request.setComments("Approved by QA Automation SOAP API Call");
    }

    /**
     * Set default values for approval process work item request to be sent via API
     * to reject SFDC record.
     *
     * @param request      ProcessWorkitemRequest to set up
     * @param piWorkItemId ID of the ProcessInstanceWorkitem record that's related to SFDC record to be rejected
     */
    public static void setFieldsForRejectAction(ProcessWorkitemRequest request, String piWorkItemId) {
        request.setWorkitemId(piWorkItemId);
        request.setAction(REJECT_ACTION);
        request.setComments("Rejected by QA Automation SOAP API Call");
    }
}
