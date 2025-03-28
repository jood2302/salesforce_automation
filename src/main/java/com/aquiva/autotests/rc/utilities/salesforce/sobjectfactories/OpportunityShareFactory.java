package com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories;

import com.sforce.soap.enterprise.sobject.OpportunityShare;

/**
 * Factory class for creating quick instances of {@link OpportunityShare} class.
 * <br/>
 * All factory methods also insert created objects into the SF database.
 */
public class OpportunityShareFactory extends SObjectFactory {

    private static final String EDIT_ACCESS_LEVEL = "Edit";

    /**
     * Create a new Opportunity Share object, and insert it into Salesforce via API.
     * <br/>
     * This will manually share the Opportunity record with the given user
     * until the ownership of the Opportunity is changed.
     *
     * @param opportunityId    ID of Opportunity to share
     * @param userIdForSharing user that the given Opportunity is shared with
     * @return OpportunityShare object with default parameters and ID from Salesforce
     * @throws Exception in case of malformed query, DB or network errors.
     */
    public static OpportunityShare shareOpportunity(String opportunityId, String userIdForSharing) throws Exception {
        var opportunityShare = new OpportunityShare();
        opportunityShare.setOpportunityId(opportunityId);
        opportunityShare.setUserOrGroupId(userIdForSharing);
        opportunityShare.setOpportunityAccessLevel(EDIT_ACCESS_LEVEL);

        CONNECTION_UTILS.insertAndGetIds(opportunityShare);

        return opportunityShare;
    }
}
