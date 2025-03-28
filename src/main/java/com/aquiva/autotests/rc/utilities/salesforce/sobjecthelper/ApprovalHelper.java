package com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper;

import com.sforce.soap.enterprise.sobject.Approval__c;
import com.sforce.soap.enterprise.sobject.ContentVersion;
import com.sforce.ws.ConnectionException;

import java.util.Arrays;
import java.util.Calendar;

import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ContentVersionHelper.getContentDocumentIdOnFile;
import static java.lang.Double.valueOf;
import static java.util.stream.Collectors.joining;

/**
 * Helper class to facilitate operations on {@link Approval__c} objects.
 */
public class ApprovalHelper extends SObjectHelper {
    //  SFDC API parameters
    public static final String S_OBJECT_API_NAME = "Approval__c";
    public static final String INVOICING_REQUEST_RECORD_TYPE = "Invoicing Request";
    public static final String INVOICE_ON_BEHALF_REQUEST_RECORD_TYPE = "Invoice-on-behalf Request";
    public static final String POC_APPROVAL_RECORD_TYPE = "POC Account";
    public static final String TEA_APPROVAL_RECORD_TYPE = "Tax Exempt Approval";
    public static final String KYC_APPROVAL_RECORD_TYPE = "KYC Approval Request";
    public static final String DIRECT_DEBIT_APPROVAL_RECORD_TYPE = "Direct Debit Request";
    public static final String CREDIT_LIMIT_INCREASE_RECORD_TYPE = "Credit Limit Increase";

    //  Default values for approval's fields
    public static final String DEFAULT_APPROVAL_NAME = "TestApproval";

    //  Default values for invoicing fields
    public static final String INDUSTRY = "Communications";
    public static final String INVOICE_TERMS = "1";
    public static final String POTENTIAL_USERS = "3";
    public static final String PAYMENT_TERMS = "30";
    public static final String ACCOUNTS_PAYABLE_EMAIL_ADDRESS = "test@email.com";
    public static final String WHY_RINGCENTRAL_SHOULD_INVOICE_CUSTOMER = "why rc should invoice";
    public static final String REASON_CUSTOMER_IS_REQUESTING_INVOICE = "reason why rc should invoice";
    public static final String INITIAL_NUMBER_OF_USERS = "2";
    public static final String PRICE_PER_USER = "4";
    public static final String INITIAL_NUMBER_OF_DEVICES = "5";
    public static final Double MONTHLY_CREDIT_LIMIT = 100000.00;
    public static final String LEGAL_COMPANY_NAME_HEAD_OFFICE = "Test Company";
    public static final Double SIGN_UP_PURCHASE_LIMIT = 100000.00;

    //  Default address values (for 'Legal_Physical_Address' fields)
    public static final String ADDRESS_COUNTRY = "USA";
    public static final String ADDRESS_STATE_PROVINCE = "NY";
    public static final String ADDRESS_CITY = "New York";
    public static final String ADDRESS_STREET = "Wall street";
    public static final String ADDRESS_ZIP_CODE = "690690";

    //  For 'Status__c' field
    public static final String APPROVAL_STATUS_APPROVED = "Approved";
    public static final String APPROVAL_STATUS_PENDING_L1 = "PendingL1Approval";
    public static final String APPROVAL_STATUS_PENDING = "Pending Approval";
    public static final String APPROVAL_STATUS_COMPLETED = "Completed";
    public static final String APPROVAL_STATUS_REJECTED = "Rejected";
    public static final String APPROVAL_STATUS_NEW = "New";

    //  For 'POC_Application_Form__c' field (POC approvals)
    public static final String LINK_TO_SIGNED_EVALUATION_AGREEMENT = "https://example.com";

    //  For 'SalesTaxExemption__c'/'LocalTaxExemption__c' field
    public static final String TAX_EXEMPTION_APPROVED = "Approved";
    public static final String TAX_EXEMPTION_REQUESTED = "Requested";

    //  For KYC Approval 'KycFiles__c' field
    public static final String KYC_FILES_DELIMITER = ";";
    private static final int KYC_FILES_NUMBER = 9;
    private static final int SIGNED_OFF_PARTICIPATION_AGREEMENT_INDEX = 3;
    private static final int GST_NUMBER_AGREEMENT_INDEX = 4;
    private static final int SEZ_CERTIFICATE_INDEX = 5;

    //  For KYC Approval 'PurposeForProcuringTheService__c' field
    public static final String COMPANY_COMMUNICATIONS_PURPOSE_VALUE = "Company Communications";
    public static final String DOMESTIC_CALL_CENTER_PURPOSE_VALUE = "Domestic Call Center";

    //  For KYC Approval 'GenderOfAuthorisedSignatory__c' field
    public static final String GENDER_MALE_VALUE = "Male";

    //  For KYC Approval 'TypeOfCustomer__c' field
    public static final String B2B_CUSTOMER_TYPE = "B2B";

    //  For KYC Approval 'Gst__c' field
    public static final String GST_REGULAR_TYPE = "Regular";
    
    //  For Credit Limit Increase Approval 'Option__c' field
    public static final String AUTOMATIC_OPTION = "Automatic";
    
    //  For Credit Limit Increase Approval 'NGBSCurrentSpendingLimit__c' field
    public static final Double NGBS_CURRENT_SPENDING_LIMIT = 50000.00;

    /**
     * Set default values to basic Approval__c fields that may be useful in tests.
     *
     * @param approval Approval__c instance to set up with default values
     */
    public static void setDefaultApprovalFields(Approval__c approval) {
        approval.setInvoice_Terms__c(INVOICE_TERMS);
        approval.setPotential_Users__c(valueOf(POTENTIAL_USERS));
        approval.setPayment_Terms__c(PAYMENT_TERMS);
        approval.setAccounts_Payable_Email_Address__c(ACCOUNTS_PAYABLE_EMAIL_ADDRESS);
        approval.setWhy_RingCentral_Should_Invoice_Customer__c(WHY_RINGCENTRAL_SHOULD_INVOICE_CUSTOMER);
        approval.setReason_Customer_is_Requesting_Invoicing__c(REASON_CUSTOMER_IS_REQUESTING_INVOICE);
        approval.setInitial_Number_of_Users__c(valueOf(INITIAL_NUMBER_OF_USERS));
        approval.setPrice_per_User__c(valueOf(PRICE_PER_USER));
        approval.setInitial_Number_of_Devices__c(valueOf(INITIAL_NUMBER_OF_DEVICES));
        approval.setIndustry__c(INDUSTRY);

        approval.setLegal_Company_Name_Head_Office__c(LEGAL_COMPANY_NAME_HEAD_OFFICE);
        approval.setLegal_Physical_Address_Country__c(ADDRESS_COUNTRY);
        approval.setLegal_Physical_Address_State_Province__c(ADDRESS_STATE_PROVINCE);
        approval.setLegal_Physical_Address_City__c(ADDRESS_CITY);
        approval.setLegal_Physical_Address_Street__c(ADDRESS_STREET);
        approval.setLegal_Physical_Address_Zip_Code__c(ADDRESS_ZIP_CODE);

        approval.setName(DEFAULT_APPROVAL_NAME);
    }

    /**
     * Set values of Monthly and SignUp limits on Approval__c record
     * according to the related Quote type (Single-product or Multi-product).
     *
     * @param approval       Approval__c instance to set up Monthly and SignUp limits
     * @param isMultiproduct true in case of approval for the Multi-product Quote
     *                       and false for the Single-product Quote
     */
    public static void setMonthlyAndSignUpLimits(Approval__c approval, boolean isMultiproduct) {
        if (isMultiproduct) {
            //  Monthly_Credit_Limit__c and Sign_Up_Purchase_Limit__c are auto-calculated based on these MP limits
            approval.setMonthly_Credit_Limit_Office__c(MONTHLY_CREDIT_LIMIT + 1);
            approval.setMonthly_Credit_Limit_Engage_Digital__c(MONTHLY_CREDIT_LIMIT + 2);
            approval.setMonthly_Credit_Limit_Engage_Voice__c(MONTHLY_CREDIT_LIMIT + 3);
            approval.setMonthly_Credit_Limit_Contact_Center__c(MONTHLY_CREDIT_LIMIT + 4);
            approval.setMonthlyCreditLimitProServ__c(MONTHLY_CREDIT_LIMIT + 5);
            approval.setSign_Up_Purchase_Limit_Office__c(SIGN_UP_PURCHASE_LIMIT + 1);
            approval.setSign_Up_Purchase_Limit_Engage_Digital__c(SIGN_UP_PURCHASE_LIMIT + 2);
            approval.setSign_Up_Purchase_Limit_Engage_Voice__c(SIGN_UP_PURCHASE_LIMIT + 3);
            approval.setSign_Up_Purchase_Limit_Contact_Center__c(SIGN_UP_PURCHASE_LIMIT + 4);
            approval.setSignUpPurchaseLimitProServ__c(SIGN_UP_PURCHASE_LIMIT + 5);
        } else {
            approval.setMonthly_Credit_Limit__c(MONTHLY_CREDIT_LIMIT + 123);
            approval.setSign_Up_Purchase_Limit__c(SIGN_UP_PURCHASE_LIMIT + 456);
        }
    }

    /**
     * Set 'Invoicing Request' record type for the Approval__c object.
     *
     * @param approval Approval__c object to set up Record type on.
     * @throws ConnectionException in case of errors while accessing API.
     */
    public static void setInvoicingRequestRecordType(Approval__c approval) throws ConnectionException {
        var invoicingRequestRecordTypeId = CONNECTION_UTILS.getRecordTypeId(S_OBJECT_API_NAME, INVOICING_REQUEST_RECORD_TYPE);
        approval.setRecordTypeId(invoicingRequestRecordTypeId);
    }

    /**
     * Set 'Invoice-on-behalf Request' record type for the Approval__c object.
     *
     * @param approval Approval__c object to set up Record type on.
     * @throws ConnectionException in case of errors while accessing API.
     */
    public static void setInvoiceOnBehalfRecordType(Approval__c approval) throws ConnectionException {
        var invoiceOnBehalfRequestRecordTypeId = CONNECTION_UTILS.getRecordTypeId(S_OBJECT_API_NAME, INVOICE_ON_BEHALF_REQUEST_RECORD_TYPE);
        approval.setRecordTypeId(invoiceOnBehalfRequestRecordTypeId);
    }

    /**
     * Set 'Tax Exempt Approval' record type for the Approval__c object.
     *
     * @param approval Approval object to set up with RecordTypeId
     * @throws ConnectionException in case of errors while accessing API
     */
    public static void setTaxExemptApprovalRecordType(Approval__c approval) throws ConnectionException {
        var taxExemptApprovalRecordTypeId = CONNECTION_UTILS.getRecordTypeId(S_OBJECT_API_NAME, TEA_APPROVAL_RECORD_TYPE);
        approval.setRecordTypeId(taxExemptApprovalRecordTypeId);
    }

    /**
     * Set 'KYC Approval Request' record type for the Approval__c object.
     *
     * @param approval Approval object to set up with RecordTypeId
     * @throws ConnectionException in case of errors while accessing API
     */
    public static void setKycApprovalRequestRecordType(Approval__c approval) throws ConnectionException {
        var kycApprovalRequestRecordTypeId = CONNECTION_UTILS.getRecordTypeId(S_OBJECT_API_NAME, KYC_APPROVAL_RECORD_TYPE);
        approval.setRecordTypeId(kycApprovalRequestRecordTypeId);
    }

    /**
     * Set 'Direct Debit Request' record type for the Approval__c object.
     *
     * @param approval Approval object to set up with RecordTypeId
     * @throws ConnectionException in case of errors while accessing API
     */
    public static void setDirectDebitApprovalRequestRecordType(Approval__c approval) throws ConnectionException {
        var directDebitApprovalRequestRecordTypeId = CONNECTION_UTILS.getRecordTypeId(S_OBJECT_API_NAME,
                DIRECT_DEBIT_APPROVAL_RECORD_TYPE);
        approval.setRecordTypeId(directDebitApprovalRequestRecordTypeId);
    }

    /**
     * Set 'Credit Limit Increase' record type for the Approval__c object.
     *
     * @param approval Approval object to set up with RecordTypeId
     * @throws ConnectionException in case of errors while accessing API
     */
    public static void setCreditLimitIncreaseApprovalRecordType(Approval__c approval) throws ConnectionException {
        var creditLimitIncreaseApprovalRecordTypeId = CONNECTION_UTILS.getRecordTypeId(S_OBJECT_API_NAME,
                CREDIT_LIMIT_INCREASE_RECORD_TYPE);
        approval.setRecordTypeId(creditLimitIncreaseApprovalRecordTypeId);
    }

    /**
     * Add Signed Off Participation Agreement file with a default Date of Sign Off (current date)
     * to the KYC Approval.
     * <br/>
     * Note: make sure to update the approval via API afterwards!
     *
     * @param kycApproval                               Approval__c instance to set up with Agreement
     * @param signedOffParticipationAgreementAttachment SFDC attachment for the Signed Off Participation Agreement
     * @throws Exception in case of I/O exceptions, DB or network errors.
     */
    public static void addSignedOffParticipationAgreement(Approval__c kycApproval,
                                                          ContentVersion signedOffParticipationAgreementAttachment)
            throws Exception {
        var signOffAgreementContentDocumentId = getContentDocumentIdOnFile(signedOffParticipationAgreementAttachment);
        setKycFilesFieldWithNewFile(kycApproval, signOffAgreementContentDocumentId, SIGNED_OFF_PARTICIPATION_AGREEMENT_INDEX);

        kycApproval.setDateOfSignOff__c(Calendar.getInstance());
    }

    /**
     * Add GST Number (and Certificate file) with a default Date of Sign Off (current date)
     * to the KYC Approval.
     * <br/>
     * Note: make sure to update the approval via API afterwards!
     *
     * @param kycApproval         Approval__c instance (KYC) to set up with GST Number
     * @param gstNumber           GST Number (e.g "29ADLFS9935J1Z4")
     * @param gstNumberAttachment SFDC Attachment for the GST Number Certificate in the resource attachments folder
     * @throws Exception in case of I/O exceptions, DB or network errors.
     */
    public static void addGstNumber(Approval__c kycApproval,
                                    String gstNumber, ContentVersion gstNumberAttachment)
            throws Exception {
        var gstNumberContentDocumentId = getContentDocumentIdOnFile(gstNumberAttachment);
        setKycFilesFieldWithNewFile(kycApproval, gstNumberContentDocumentId, GST_NUMBER_AGREEMENT_INDEX);

        kycApproval.setGstNo__c(gstNumber);
    }

    /**
     * Add SEZ Certificate file with a default Date of Sign Off (current date)
     * to the KYC Approval.
     * <br/>
     * Note: make sure to update the approval via API afterwards!
     *
     * @param kycApproval              Approval__c instance (KYC) to set up with SEZ Certificate
     * @param sezCertificateAttachment SFDC Attachment for the SEZ Certificate in the resource attachment folder
     * @throws Exception in case of I/O exceptions, DB or network errors.
     */
    public static void addSezCertificate(Approval__c kycApproval,
                                         ContentVersion sezCertificateAttachment)
            throws Exception {
        var sezCertificateContentDocumentId = getContentDocumentIdOnFile(sezCertificateAttachment);
        setKycFilesFieldWithNewFile(kycApproval, sezCertificateContentDocumentId, SEZ_CERTIFICATE_INDEX);
    }

    /**
     * Add/replace a new file in the KYC Approval by updating {@code Approval__c.KycFiles__c} field.
     * <br/><br/>
     * KYC Approval attachments are mapped to the sections on the "KYC Details" section
     * using {@code Approval__c.KycFiles__c} field.
     * ContentDocumentId values of the file attachments are organized as a list separated by ";".
     * No attachment = empty string.
     * <br/>
     * E.g. ";;;06919000002UjV2AAK;06919000002UjV7AAK;06919000002UjVCAA0;;;"
     * <br/>
     * Every file attachment has its own position (e.g. SEZ Certificate should be at 6th position, index = 5).
     *
     * @param kycApproval                 KYC Approval to set up with a new file attachment mapping
     * @param newKycFileContentDocumentId ContentDocumentId value for the new attachment
     *                                    (see {@link ContentVersion#getContentDocumentId()}).
     * @param index                       0..N position of the attachment
     *                                    (e.g. 3 for the Signed Off Participation Agreement)
     */
    private static void setKycFilesFieldWithNewFile(Approval__c kycApproval,
                                                    String newKycFileContentDocumentId, int index) {
        var kycFilesFieldValue = kycApproval.getKycFiles__c() != null ?
                kycApproval.getKycFiles__c() :
                Arrays.stream(new String[KYC_FILES_NUMBER])
                        .map(s -> EMPTY_STRING)
                        .collect(joining(KYC_FILES_DELIMITER));

        var kycDocumentIds = kycFilesFieldValue.split(KYC_FILES_DELIMITER, -1);
        kycDocumentIds[index] = newKycFileContentDocumentId;
        var kycFilesNewValue = String.join(KYC_FILES_DELIMITER, kycDocumentIds);

        kycApproval.setKycFiles__c(kycFilesNewValue);
    }
}
