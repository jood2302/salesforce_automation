package com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories;

import com.sforce.soap.enterprise.sobject.*;

import javax.annotation.Nullable;
import java.util.UUID;

import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.aquiva.autotests.rc.utilities.StringHelper.getRandomUSPhone;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.LeadHelper.*;
import static java.util.Objects.nonNull;

/**
 * Factory class for creating quick instances of {@link Lead} class.
 * <br/>
 * All factory methods also insert created objects into the SF database.
 */
public class LeadFactory extends SObjectFactory {
    //  Default values for lead's parameters
    private static final String DEFAULT_LEAD_NAME = "TestLead";
    private static final String DEFAULT_ACCOUNT_NAME = "TestAccount";

    /**
     * Create new Customer Lead record and insert it into Salesforce via API.
     *
     * @param ownerUser user to be set as Lead's owner
     * @return Lead record with default parameters and ID from Salesforce
     * @throws Exception in case of malformed query, DB or network errors.
     */
    public static Lead createCustomerLeadInSFDC(User ownerUser) throws Exception {
        return createLeadInSFDC(ownerUser, null);
    }

    /**
     * Create new Partner Lead record and insert it into Salesforce via API.
     *
     * @param ownerUser      user to be set as Lead's owner
     * @param partnerAccount related Partner Account to link with a Lead
     * @param tierName       Tier Name to be set
     * @return Lead record with default parameters and ID from Salesforce
     * @throws Exception in case of malformed query, DB or network errors.
     */
    public static Lead createPartnerLeadInSFDC(User ownerUser, Account partnerAccount, String tierName) throws Exception {
        var partnerLead = createLeadInSFDC(ownerUser, partnerAccount);

        partnerLead.setLeadPartnerID__c(partnerAccount.getPartner_ID__c());
        partnerLead.setPartner_Contact__c(partnerAccount.getPartner_Contact__c());
        partnerLead.setPartner_Account__c(partnerAccount.getId());
        partnerLead.setPhone(getRandomUSPhone());
        partnerLead.setPartner_Lead_Source__c(PARTNER_LEAD_SOURCE_TYPE);

        setDefaultAddress(partnerLead);

        partnerLead.setLead_Brand_Name__c(partnerAccount.getPermitted_Brands__c());
        partnerLead.setLead_Tier_Name__c(tierName);
        partnerLead.setCurrencyIsoCode(partnerAccount.getCurrencyIsoCode());
        partnerLead.setDescription(TEST_STRING);
        partnerLead.setHow_did_you_acquire_this_Lead__c(TEST_STRING);

        CONNECTION_UTILS.update(partnerLead);
        return partnerLead;
    }

    /**
     * Create new Lead record and insert it into Salesforce via API.
     *
     * @param ownerUser      user to be set as Lead's owner
     * @param partnerAccount {@code Account} related Partner Account to link with a Lead,
     *                       or {@code null} if no Partner Account is needed
     * @return Lead record with default parameters and ID from Salesforce
     * @throws Exception in case of malformed query, DB or network errors.
     */
    private static Lead createLeadInSFDC(User ownerUser, @Nullable Account partnerAccount) throws Exception {
        var lead = new Lead();
        lead.setFirstName(DEFAULT_LEAD_NAME);

        var uniqueId = UUID.randomUUID().toString();
        lead.setLastName(uniqueId);
        lead.setCompany(DEFAULT_ACCOUNT_NAME + " " + uniqueId);
        lead.setEmail(uniqueId + "@example.com");
        lead.setBypass_Routing__c(true); // to skip Lead re-assignment to 'LeanData Queue' user

        if (nonNull(partnerAccount)) {
            setPartnerLeadRecordType(lead);
        }

        CONNECTION_UTILS.insertAndGetIds(lead);

        lead.setOwnerId(ownerUser.getId());
        CONNECTION_UTILS.update(lead);

        return lead;
    }
}
