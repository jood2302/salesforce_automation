package com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories;

import com.sforce.soap.enterprise.sobject.*;
import com.sforce.ws.ConnectionException;

import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountContactRoleFactory.createAccountContactRole;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ContentVersionFactory.createAttachmentForSObject;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountContactRoleHelper.ACCOUNTS_PAYABLE_ROLE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.*;

/**
 * Factory class for creating quick instances of {@link Approval__c} class.
 * <br/>
 * All factory methods also insert created objects into the SF database.
 */
public class ApprovalFactory extends SObjectFactory {

    /**
     * Create Invoice Request Approval record with its default Status = 'Required',
     * and a related non-primary 'Accounts Payable' AccountContactRole record,
     * and insert them into Salesforce via API.
     *
     * @param opportunity    Opportunity object to set up
     * @param account        Account object to set up
     * @param contact        Contact object to set up
     * @param ownerId        ID of the Salesforce user that will be the owner of the record
     * @param isMultiproduct true if the approval is related to a Multiproduct Quote/Opportunity
     * @return new Invoice Request Approval record with ID from Salesforce
     * @throws ConnectionException in case of errors while accessing API
     */
    public static Approval__c createInvoiceApproval(Opportunity opportunity, Account account, Contact contact,
                                                    String ownerId, boolean isMultiproduct)
            throws ConnectionException {
        var newInvoiceRequestApproval = new Approval__c();
        newInvoiceRequestApproval.setOpportunity__c(opportunity.getId());
        newInvoiceRequestApproval.setAccount__c(account.getId());
        newInvoiceRequestApproval.setAccountPayableContact__c(contact.getId());
        newInvoiceRequestApproval.setInitial_Order_Form_Contract_Link__c(TEST_STRING);
        newInvoiceRequestApproval.setOwnerId(ownerId);

        setDefaultApprovalFields(newInvoiceRequestApproval);
        setMonthlyAndSignUpLimits(newInvoiceRequestApproval, isMultiproduct);
        setInvoicingRequestRecordType(newInvoiceRequestApproval);

        createAccountContactRole(account, contact, ACCOUNTS_PAYABLE_ROLE, false);

        CONNECTION_UTILS.insertAndGetIds(newInvoiceRequestApproval);

        return newInvoiceRequestApproval;
    }

    /**
     * Create Invoice Request Approval record with a related non-primary 'Accounts Payable' AccountContactRole record,
     * and insert them into Salesforce via API.
     * After the approval is created, its Status is set to 'Approved' via API.
     *
     * @param opportunity    Opportunity object to set up
     * @param account        Account object to set up
     * @param contact        Contact object to set up
     * @param ownerId        ID of the Salesforce user that will be the owner of the record
     * @param isMultiproduct true if the approval is related to a Multiproduct Quote/Opportunity
     * @return new Invoice Request Approval record with ID from Salesforce
     * @throws ConnectionException in case of errors while accessing API
     */
    public static Approval__c createInvoiceApprovalApproved(Opportunity opportunity, Account account, Contact contact,
                                                            String ownerId, boolean isMultiproduct)
            throws ConnectionException {
        var approvedInvoiceApproval = createInvoiceApproval(opportunity, account, contact, ownerId, isMultiproduct);

        approvedInvoiceApproval.setStatus__c(APPROVAL_STATUS_APPROVED);
        CONNECTION_UTILS.update(approvedInvoiceApproval);

        return approvedInvoiceApproval;
    }

    /**
     * Create Invoice-on-behalf Request Approval record with its default Status = 'Required',
     * and insert it into Salesforce via API.
     *
     * @param opportunity    Opportunity object to set up
     * @param account        Account object to set up
     * @param contact        Contact object to set up
     * @param partnerContact partner Contact object to set up
     *                       (Accounts Payable Contact for the Partner Account)
     * @param ownerId        ID of the Salesforce user that will be the owner of the record
     * @return new Invoice-on-behalf Request Approval record with ID from Salesforce
     * @throws ConnectionException in case of errors while accessing API
     */
    public static Approval__c createInvoiceOnBehalfApproval(
            Opportunity opportunity, Account account, Contact contact, Contact partnerContact, String ownerId) throws ConnectionException {
        var newInvoiceOnBehalfApproval = new Approval__c();
        newInvoiceOnBehalfApproval.setOpportunity__c(opportunity.getId());
        newInvoiceOnBehalfApproval.setAccount__c(account.getId());
        newInvoiceOnBehalfApproval.setAccountPayableContact__c(contact.getId());
        newInvoiceOnBehalfApproval.setPartnerAccountPayableContact__c(partnerContact.getId());
        newInvoiceOnBehalfApproval.setInitial_Order_Form_Contract_Link__c(TEST_STRING);
        newInvoiceOnBehalfApproval.setOwnerId(ownerId);

        setDefaultApprovalFields(newInvoiceOnBehalfApproval);
        setMonthlyAndSignUpLimits(newInvoiceOnBehalfApproval, false);
        setInvoiceOnBehalfRecordType(newInvoiceOnBehalfApproval);

        CONNECTION_UTILS.insertAndGetIds(newInvoiceOnBehalfApproval);

        return newInvoiceOnBehalfApproval;
    }

    /**
     * Create KYC Approval record with its default Status = 'Required',
     * and Signed Off Participation Agreement file attachment,
     * and insert them into Salesforce via API.
     *
     * @param opportunity                             Opportunity object to set up
     * @param account                                 Account object to set up
     * @param signedOffParticipationAgreementFileName file name of the Signed Off Participation Agreement
     *                                                in the resource attachments folder
     * @param ownerId                                 ID of the Salesforce user that will be the owner of the record
     * @return new KYC Approval instance with Signed Off Participation Agreement and ID from Salesforce
     * @throws Exception in case of I/O exceptions, DB or network errors.
     */
    public static Approval__c createKycApproval(Opportunity opportunity, Account account,
                                                String signedOffParticipationAgreementFileName,
                                                String ownerId)
            throws Exception {
        var newKycApproval = new Approval__c();
        newKycApproval.setOpportunity__c(opportunity.getId());
        newKycApproval.setAccount__c(account.getId());
        newKycApproval.setOwnerId(ownerId);

        setKycApprovalRequestRecordType(newKycApproval);

        CONNECTION_UTILS.insertAndGetIds(newKycApproval);

        var signOffAgreementAttachment = createAttachmentForSObject(
                newKycApproval.getId(), signedOffParticipationAgreementFileName);
        addSignedOffParticipationAgreement(newKycApproval, signOffAgreementAttachment);
        CONNECTION_UTILS.update(newKycApproval);

        return newKycApproval;
    }

    /**
     * Create Tax Exempt Approval record with its default Status = 'Required',
     * and insert it into Salesforce via API.
     * <br/>
     * Note: for India TEA Approvals the Status is set to 'Approved' automatically.
     *
     * @param accountId     ID of the Account object related to Tax Exempt Approval
     * @param opportunityId ID of the Opportunity object related to Tax Exempt Approval
     * @param contactId     ID of the Contact object related to Tax Exempt Approval
     * @param ownerId       ID of the Salesforce user that will be the owner of the record
     * @return new Tax Exempt Approval object with ID from Salesforce
     * @throws ConnectionException in case of errors while accessing API
     */
    public static Approval__c createTeaApproval(String accountId, String opportunityId, String contactId,
                                                String ownerId)
            throws ConnectionException {
        var newTeaApproval = new Approval__c();
        newTeaApproval.setName(DEFAULT_APPROVAL_NAME);

        newTeaApproval.setAccount__c(accountId);
        newTeaApproval.setOpportunity__c(opportunityId);
        newTeaApproval.setContact__c(contactId);
        newTeaApproval.setOwnerId(ownerId);

        setTaxExemptApprovalRecordType(newTeaApproval);
        CONNECTION_UTILS.insertAndGetIds(newTeaApproval);

        return newTeaApproval;
    }

    /**
     * Create Direct Debit Approval record with its default Status = 'Required'
     * and insert it into Salesforce via API.
     *
     * @param account     Account object to set up
     * @param opportunity Opportunity object to set up
     * @param ownerId     ID of the Salesforce user that will be the owner of the record
     * @return new Direct Debit Approval object with ID from Salesforce
     * @throws ConnectionException in case of errors while accessing API
     */
    public static Approval__c createDirectDebitApproval(Account account, Opportunity opportunity, String ownerId) throws ConnectionException {
        var newDirectDebitApproval = new Approval__c();
        newDirectDebitApproval.setAccount__c(account.getId());
        newDirectDebitApproval.setOpportunity__c(opportunity.getId());
        newDirectDebitApproval.setOwnerId(ownerId);

        setDirectDebitApprovalRequestRecordType(newDirectDebitApproval);
        setDefaultApprovalFields(newDirectDebitApproval);
        setMonthlyAndSignUpLimits(newDirectDebitApproval, false);
        CONNECTION_UTILS.insertAndGetIds(newDirectDebitApproval);

        return newDirectDebitApproval;
    }

    /**
     * Create Credit Limit Increase Approval record, set its fields with default values,
     * and insert it into Salesforce via API.
     *
     * @param account     Account object to set up
     * @param opportunity Opportunity object to set up
     * @param quote       Quote object to set up
     * @param ownerId     ID of the Salesforce user that will be the owner of the record
     * @return new Credit Limit Increase Approval object with ID from Salesforce
     * @throws ConnectionException in case of errors while accessing API
     */
    public static Approval__c createCreditLimitIncreaseApproval(Account account, Opportunity opportunity, Quote quote,
                                                                String ownerId) throws ConnectionException {
        var newCreditLimitIncreaseApproval = new Approval__c();
        newCreditLimitIncreaseApproval.setAccount__c(account.getId());
        newCreditLimitIncreaseApproval.setOpportunity__c(opportunity.getId());
        newCreditLimitIncreaseApproval.setQuote__c(quote.getId());
        newCreditLimitIncreaseApproval.setOwnerId(ownerId);

        setCreditLimitIncreaseApprovalRecordType(newCreditLimitIncreaseApproval);
        setDefaultApprovalFields(newCreditLimitIncreaseApproval);
        setMonthlyAndSignUpLimits(newCreditLimitIncreaseApproval, false);

        newCreditLimitIncreaseApproval.setJustification__c(TEST_STRING);
        newCreditLimitIncreaseApproval.setOption__c(AUTOMATIC_OPTION);
        newCreditLimitIncreaseApproval.setNGBSCurrentSpendingLimit__c(NGBS_CURRENT_SPENDING_LIMIT);
        newCreditLimitIncreaseApproval.setNGBSAmountToIncrease__c(1.00);

        CONNECTION_UTILS.insertAndGetIds(newCreditLimitIncreaseApproval);

        return newCreditLimitIncreaseApproval;
    }
}
