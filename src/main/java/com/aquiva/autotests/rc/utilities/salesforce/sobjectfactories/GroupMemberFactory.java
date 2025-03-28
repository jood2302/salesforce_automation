package com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories;

import com.sforce.soap.enterprise.sobject.*;
import com.sforce.ws.ConnectionException;

/**
 * Factory class for creating quick instances of {@link GroupMember} class.
 * <br/>
 * All factory methods also insert created objects into the SF database.
 */
public class GroupMemberFactory extends SObjectFactory {

    /**
     * Create a Group Member for the 'KYC Onboarding Queue'.
     * This will enable the given user to edit the {@link Approval__c} record
     * even if it's 'locked' by the standard approval process.
     *
     * @param userId ID of the User to be added as a Group Member
     * @return new Group Member instance with ID from Salesforce
     * @throws ConnectionException in case of errors while accessing API
     */
    public static GroupMember createGroupMemberForKycQueue(String userId) throws ConnectionException {
        var kycQueue = CONNECTION_UTILS.querySingleRecord(
                "SELECT Id " +
                        "FROM Group " +
                        "WHERE Name = 'KYC Onboarding Queue' " +
                        "AND Type = 'Queue'",
                Group.class);

        var kycQueueGroupMember = new GroupMember();
        kycQueueGroupMember.setUserOrGroupId(userId);
        kycQueueGroupMember.setGroupId(kycQueue.getId());

        CONNECTION_UTILS.insertAndGetIds(kycQueueGroupMember);

        return kycQueueGroupMember;
    }
}
