package ngbs.approvals;

import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import com.sforce.ws.ConnectionException;

import java.io.File;
import java.util.Calendar;
import java.util.*;

import static base.Pages.kycApprovalPage;
import static base.Pages.wizardPage;
import static com.aquiva.autotests.rc.utilities.FileUtils.getAttachmentResourceFile;
import static com.aquiva.autotests.rc.utilities.StringHelper.getRandomPositiveInteger;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.GroupMemberFactory.createGroupMemberForKycQueue;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.refresh;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static java.util.Calendar.NOVEMBER;

/**
 * Test methods for working with KYC Approval records
 * (e.g. populating KYC fields to prepare it for approval process).
 */
public class KycApprovalSteps {
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    public final String identityDocumentOfCompanyDocumentNumberValue;
    public final String identityDocumentOfCompanyPlaceOfIssueValue;
    public final String identityDocumentOfCompanyIssuingAuthorityValue;
    public final File identityDocumentOfCompanyFile;

    public final String photoIdProofTypeDocumentNumberValue;
    public final String photoIdProofTypePlaceOfIssueValue;
    public final String photoIdProofTypeIssuingAuthorityValue;
    public final File photoIdProofTypeFile;

    public final String authorizationLetterDocumentNumberValue;
    public final String authorizationLetterPlaceOfIssueValue;
    public final String authorizationLetterIssuingAuthorityValue;
    public final File authorizationLetterFile;

    public final String signedOffParticipationAgreementFileName;

    public final String gstNumber;
    public final String gstStateCodeOfCustomer;
    public final String gstStateNameOfCustomer;
    public final String gstNumberFileName;

    public final String sezCertificateExemptionFileName;
    public final File panGirNumberFile;
    public final File addressOfTheAuthorisedSignatoryFile;
    public final File passportOfTheAuthorisedSignatoryFile;

    public final List<String> purposeForProcuringTheServiceTestValues;
    public final Calendar dateOfBirthOfAuthorisedSignatoryTestValue;

    public final String panGirNoTestValue;
    public final String testPinCodeValue;

    /**
     * New instance for the class with the test methods/steps for working with KYC Approval records.
     */
    public KycApprovalSteps() {
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        identityDocumentOfCompanyDocumentNumberValue = getRandomPositiveInteger();
        identityDocumentOfCompanyPlaceOfIssueValue = "First Place of Issue";
        identityDocumentOfCompanyIssuingAuthorityValue = "1st Authority";
        identityDocumentOfCompanyFile = getAttachmentResourceFile("test1.jpg");

        photoIdProofTypeDocumentNumberValue = getRandomPositiveInteger();
        photoIdProofTypePlaceOfIssueValue = "Second Place of Issue";
        photoIdProofTypeIssuingAuthorityValue = "2nd Authority";
        photoIdProofTypeFile = getAttachmentResourceFile("test2.jpg");

        authorizationLetterDocumentNumberValue = getRandomPositiveInteger();
        authorizationLetterPlaceOfIssueValue = "Third Place of Issue";
        authorizationLetterIssuingAuthorityValue = "3rd Authority";
        authorizationLetterFile = getAttachmentResourceFile("test3.jpg");

        signedOffParticipationAgreementFileName = "test4.jpg";

        //  all GST values should correspond to the valid data from NGBS for India states
        gstNumber = "29ADLFS9935J1Z4";
        gstStateCodeOfCustomer = "29";
        //  should be the same as Billing State in the KYC Details section
        gstStateNameOfCustomer = "Karnataka";
        gstNumberFileName = "test5.jpg";

        sezCertificateExemptionFileName = "test6.jpg";
        panGirNumberFile = getAttachmentResourceFile("test7.png");
        addressOfTheAuthorisedSignatoryFile = getAttachmentResourceFile("test8.png");
        passportOfTheAuthorisedSignatoryFile = getAttachmentResourceFile("rc.png");

        purposeForProcuringTheServiceTestValues = List.of(COMPANY_COMMUNICATIONS_PURPOSE_VALUE,
                DOMESTIC_CALL_CENTER_PURPOSE_VALUE);
        dateOfBirthOfAuthorisedSignatoryTestValue = Calendar.getInstance();
        dateOfBirthOfAuthorisedSignatoryTestValue.set(1993, NOVEMBER, 17);

        panGirNoTestValue = "3339988001";
        testPinCodeValue = "123456";
    }

    /**
     * Get a user with 'Sales Rep - Lightning' profile with 'Edit KYC Approval' permission set,
     * and create GroupMember record for them if necessary (to be able to edit KYC Approval).
     */
    public User getSalesUserWithKycApprovalPermissionSet() {
        return step("Find a user with 'Sales Rep - Lightning' profile with 'Edit KYC Approval' permission set", () -> {
            var salesUserWithEditKycApprovalPermissionSet = getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    .withPermissionSet(KYC_APPROVAL_EDIT_PS)
                    .execute();
            createGroupMemberForKycQueue(salesUserWithEditKycApprovalPermissionSet.getId());

            return salesUserWithEditKycApprovalPermissionSet;
        });
    }

    /**
     * Find a default KYC Approval created for the given Account,
     * and transfer its ownership to the user with 'Sales Rep - Lightning' profile
     * and 'KYC_Approval_Edit' Permission Set.
     * <br/>
     * Note: there should be only ONE KYC Approval related to the Account at this point
     * (usually, happens right after the creating the test Opportunity via API).
     *
     * @param salesUserWithEditKycApprovalPermissionSetId ID of the user to be the new KYC Approval's owner
     * @param accountId                                   ID of the Account that KYC Approval is related to
     * @return KYC Approval record that now belongs to the given user
     */
    public Approval__c changeOwnerOfDefaultKycApproval(String salesUserWithEditKycApprovalPermissionSetId, String accountId) {
        return step("Transfer the ownership of the KYC Approval " +
                "to the user with a 'Sales Rep - Lightning' profile and 'KYC_Approval_Edit' Permission Set via API", () -> {
            var kycApproval = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM Approval__c " +
                            "WHERE Account__c = '" + accountId + "' " +
                            "AND RecordType.Name = '" + KYC_APPROVAL_RECORD_TYPE + "'",
                    Approval__c.class);

            kycApproval.setOwnerId(salesUserWithEditKycApprovalPermissionSetId);
            enterpriseConnectionUtils.update(kycApproval);

            return kycApproval;
        });
    }

    /**
     * Populate KYC Approval's required fields from the 'Authorised Signatory Details'
     * and 'Connection/Service Details' sections via SFDC API,
     * populate all the fields and add all the file attachments to some 'KYC Details' block's sections
     * to prepare KYC record for approval.
     *
     * @param kycApproval KYC Approval record to set up
     */
    public void populateKycApprovalFieldsRequiredForApproval(Approval__c kycApproval) {
        step("Populate required fields from 'Authorised Signatory Details' section " +
                "and 'Purpose for procuring the Service' field on the KYC Approval via API", () -> {
            kycApproval.setNameOfTheAuthorisedSignatory__c(UUID.randomUUID().toString());
            kycApproval.setNameOfFatherOrHusband__c(UUID.randomUUID().toString());
            kycApproval.setGenderOfAuthorisedSignatory__c(GENDER_MALE_VALUE);
            kycApproval.setDateOfBirthOfAuthorisedSignatory__c(dateOfBirthOfAuthorisedSignatoryTestValue);
            //  necessary to set as it's populated only when KYC was created automatically
            //  after Opportunity creation via QOP
            kycApproval.setNationalityOfAuthorisedSignatory__c(INDIA_BILLING_COUNTRY);
            kycApproval.setPurposeForProcuringTheService__c(String.join(";", purposeForProcuringTheServiceTestValues));
            kycApproval.setHouseNo__c(UUID.randomUUID().toString());
            kycApproval.setArea__c(UUID.randomUUID().toString());
            kycApproval.setStreetAddress__c(INDIA_BILLING_STREET);
            kycApproval.setCity__c(INDIA_BILLING_CITY);
            kycApproval.setDistrict__c(INDIA_BILLING_CITY);
            kycApproval.setState__c(INDIA_BILLING_STATE);
            kycApproval.setPanGirNo__c(panGirNoTestValue);
            kycApproval.setPinCode__c(testPinCodeValue);

            enterpriseConnectionUtils.update(kycApproval);
        });

        step("Upload attachments and populate all fields in " +
                "'Identity document of company', 'Photo ID Proof Type of Authorised Signatory', " +
                "'Authorisation Letter for Authorised Signatory', 'PAN/GIR No.', " +
                "'Address of the Authorised Signatory', 'Passport of the Authorised Signatory' sections " +
                "in the 'KYC Details' block, and save changes", () -> {
            kycApprovalPage.identityDocumentOfCompanyUploadInput
                    .shouldBe(visible, ofSeconds(30))
                    .uploadFile(identityDocumentOfCompanyFile);
            kycApprovalPage.identityDocumentOfCompanyDocumentNoInput.setValue(identityDocumentOfCompanyDocumentNumberValue);
            kycApprovalPage.identityDocumentOfCompanyPlaceOfIssueInput.setValue(identityDocumentOfCompanyPlaceOfIssueValue);
            kycApprovalPage.identityDocumentOfCompanyIssuingAuthorityInput
                    .setValue(identityDocumentOfCompanyIssuingAuthorityValue);
            kycApprovalPage.identityDocumentOfCompanyDateOfIssueInput.setTomorrowDate();

            kycApprovalPage.photoIdProofTypeUploadInput.uploadFile(photoIdProofTypeFile);
            kycApprovalPage.photoIdProofTypeSection.click();
            kycApprovalPage.photoIdProofTypeDocumentNoInput.setValue(photoIdProofTypeDocumentNumberValue);
            kycApprovalPage.photoIdProofTypePlaceOfIssueInput.setValue(photoIdProofTypePlaceOfIssueValue);
            kycApprovalPage.photoIdProofTypeIssuingAuthorityInput.setValue(photoIdProofTypeIssuingAuthorityValue);
            kycApprovalPage.photoIdProofTypeDateOfIssueInput.setTomorrowDate();

            kycApprovalPage.authorisationLetterUploadInput.uploadFile(authorizationLetterFile);
            kycApprovalPage.authorisationLetterSection.click();
            kycApprovalPage.authorisationLetterDocumentNoInput.setValue(authorizationLetterDocumentNumberValue);
            kycApprovalPage.authorisationLetterPlaceOfIssueInput.setValue(authorizationLetterPlaceOfIssueValue);
            kycApprovalPage.authorisationLetterIssuingAuthorityInput.setValue(authorizationLetterIssuingAuthorityValue);
            kycApprovalPage.authorisationLetterDateOfIssueInput.setTomorrowDate();

            kycApprovalPage.panGirNumberUploadInput.uploadFile(panGirNumberFile);

            kycApprovalPage.addressOfTheAuthorisedSignatoryUploadInput.uploadFile(addressOfTheAuthorisedSignatoryFile);

            kycApprovalPage.passportOfTheAuthorisedSignatoryUploadInput.uploadFile(passportOfTheAuthorisedSignatoryFile);

            kycApprovalPage.kycDetailsSaveChanges();
        });
    }

    /**
     * Expand 'Billing Address' section in 'KYC Details' block and populate all the fields in it
     * with some default values (valid address in India).
     */
    public void populateBillingAddressSectionWithIndiaAddress() {
        populateBillingAddressSectionWithIndiaAddress(INDIA_BILLING_STREET, INDIA_BILLING_CITY, INDIA_BILLING_STATE, INDIA_BILLING_POSTAL_CODE);
    }

    /**
     * Expand 'Billing Address' section in 'KYC Details' block and populate all the fields in it.
     *
     * @param billingStreet     any valid India's street (e.g. "85 Nawab Hyder Ali Khan Road")
     * @param billingCity       any valid India's city (e.g. "Bangaluru")
     * @param billingState      any valid India's state (e.g. "Karnataka")
     * @param billingPostalCode any valid India's postal code (e.g. "560002")
     */
    public void populateBillingAddressSectionWithIndiaAddress(String billingStreet, String billingCity,
                                                              String billingState, String billingPostalCode) {
        kycApprovalPage.billingAddressSection.click();
        kycApprovalPage.billingCountryInput
                .shouldBe(enabled, ofSeconds(10))
                .setValue(INDIA_BILLING_COUNTRY);
        kycApprovalPage.billingStreetInput.setValue(billingStreet);
        kycApprovalPage.billingCityInput.setValue(billingCity);
        kycApprovalPage.billingStateInput.setValue(billingState);
        kycApprovalPage.billingPostalCodeInput.setValue(billingPostalCode)
                .unfocus();
    }

    /**
     * Set Quote.Enabled_LBO__c = true for the main Quote of the given Opportunity,
     * and refresh the currently opened Quote Wizard/UQT window with the Quote.
     * <br/>
     * This method may be helpful when a user needs to skip required devices assignments
     * for closing / signing up the Opportunity.
     * And the reopening helps to update the current quote state with Enabled_LBO__c = true.
     *
     * @param opportunityId ID of the Opportunity related to the Quote that needs to be enabled for LBO
     * @throws ConnectionException in case of errors while accessing API
     */
    public void reopenQuoteWizardWithEnabledLBO(String opportunityId) throws ConnectionException {
        var quote = enterpriseConnectionUtils.querySingleRecord(
                "SELECT Id " +
                        "FROM Quote " +
                        "WHERE OpportunityId = '" + opportunityId + "' ",
                Quote.class);
        quote.setEnabled_LBO__c(true);
        enterpriseConnectionUtils.update(quote);

        refresh();
        wizardPage.waitUntilLoaded();
    }
}
