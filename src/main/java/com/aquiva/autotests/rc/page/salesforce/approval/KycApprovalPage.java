package com.aquiva.autotests.rc.page.salesforce.approval;

import com.aquiva.autotests.rc.page.components.LightningDatepicker;
import com.aquiva.autotests.rc.page.salesforce.approval.modal.KycDetailsAttachmentsModal;
import com.codeborne.selenide.SelenideElement;
import com.sforce.soap.enterprise.sobject.Approval__c;

import java.time.format.DateTimeFormatter;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$x;
import static java.time.Duration.ofSeconds;

/**
 * The Standard Salesforce page that displays KYC Approval ({@link Approval__c}) record information,
 * such as Approval details (Approval Name, Approval's Account, etc...), action buttons,
 * KYC Details and many more.
 */
public class KycApprovalPage extends ApprovalPage {

    public static final String MULTIPLE_FILES_LABEL_PATTERN = "View All (%d)";

    public static final DateTimeFormatter KYC_DETAILS_DATES_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");

    //  Error notification messages texts
    public static final String INCORRECT_INDIA_STATE_ERROR = "State is incorrect. Please provide valid India state";
    public static final String INCORRECT_GST_NUMBER_ERROR = "Provided GST number is incorrect";
    public static final String GST_NUMBER_DOES_NOT_MATCH_BILLING_STATE_ERROR = "Provided GST number doesn't match " +
            "the billing state on the account. Billing state for provided GST number should be";
    public static final String APPROVAL_WAS_APPROVED_MESSAGE = "Approval was approved.";

    public static final String MISSING_SIGNED_OFF_DATE_ERROR = "Enter date before saving, field is required";

    //  For 'Billing State' field on the 'KYC Details' block
    public static final String GUJARAT_SHORT_BILLING_STATE = "GJ";

    //  Related list headers
    public static final String LOCAL_SUBSCRIBED_ADDRESSES_RELATED_LIST = "Local Subscribed Addresses";

    //  'KYC Details' block
    public final SelenideElement kycDetailsBlock = $x("//*[@data-component-id='kycDetails']");
    public final SelenideElement kycDetailsSaveButton = kycDetailsBlock.$x(".//button[text()='Save']");
    public final SelenideElement kycDetailsCancelButton = kycDetailsBlock.$x(".//button[text()='Cancel']");
    public final SelenideElement kycDetailsSpinner = kycDetailsBlock.$x(".//*[@role='status']");

    //  'Identity document of company' section in 'KYC Details' block
    public final SelenideElement identityDocumentOfCompanySection =
            kycDetailsBlock.$x(".//*[@data-id='identityDocumentSection']");
    public final SelenideElement identityDocumentOfCompanyUploadInput =
            identityDocumentOfCompanySection.$x(".//input[@type='file']");
    public final SelenideElement identityDocumentOfCompanyDocumentNoInput =
            identityDocumentOfCompanySection.$x(".//*[@data-id='document-no']//input");
    public final SelenideElement identityDocumentOfCompanyPlaceOfIssueInput =
            identityDocumentOfCompanySection.$x(".//*[@data-id='place-of-issue']//input");
    public final SelenideElement identityDocumentOfCompanyIssuingAuthorityInput =
            identityDocumentOfCompanySection.$x(".//*[@data-id='issuing-authority']//input");
    public final LightningDatepicker identityDocumentOfCompanyDateOfIssueInput =
            new LightningDatepicker(identityDocumentOfCompanySection.$x(".//lightning-datepicker"));
    public final SelenideElement identityDocumentOfCompanyUploadedFilesLink =
            identityDocumentOfCompanySection.$(".file-details .slds-text-link");

    //  'Photo ID Proof Type of Authorised Signatory' section in 'KYC details' block
    public final SelenideElement photoIdProofTypeSection = kycDetailsBlock.$x(".//*[@data-id='photoIdDocumentSection']");
    public final SelenideElement photoIdProofTypeUploadInput =
            photoIdProofTypeSection.$x(".//input[@type='file']");
    public final SelenideElement photoIdProofTypeDocumentNoInput =
            photoIdProofTypeSection.$x(".//*[@data-id='document-no']//input");
    public final SelenideElement photoIdProofTypePlaceOfIssueInput =
            photoIdProofTypeSection.$x(".//*[@data-id='place-of-issue']//input");
    public final SelenideElement photoIdProofTypeIssuingAuthorityInput =
            photoIdProofTypeSection.$x(".//*[@data-id='issuing-authority']//input");
    public final LightningDatepicker photoIdProofTypeDateOfIssueInput =
            new LightningDatepicker(photoIdProofTypeSection.$x(".//lightning-datepicker"));
    public final SelenideElement photoIdProofTypeUploadedFilesLink = photoIdProofTypeSection.$(".file-details .slds-text-link");

    //  'Authorisation Letter for Authorised Signatory' section in 'KYC details' block
    public final SelenideElement authorisationLetterSection =
            kycDetailsBlock.$x(".//*[@data-id='authorisationLetterDocumentSection']");
    public final SelenideElement authorisationLetterUploadInput =
            authorisationLetterSection.$x(".//input[@type='file']");
    public final SelenideElement authorisationLetterDocumentNoInput =
            authorisationLetterSection.$x(".//*[@data-id='document-no']//input");
    public final SelenideElement authorisationLetterPlaceOfIssueInput =
            authorisationLetterSection.$x(".//*[@data-id='place-of-issue']//input");
    public final SelenideElement authorisationLetterIssuingAuthorityInput =
            authorisationLetterSection.$x(".//*[@data-id='issuing-authority']//input");
    public final LightningDatepicker authorisationLetterDateOfIssueInput =
            new LightningDatepicker(authorisationLetterSection.$x(".//lightning-datepicker"));

    //  'Signed Off Participation Agreement' section in 'KYC Details' block
    public final SelenideElement signedOffParticipationAgreementSection =
            kycDetailsBlock.$x(".//div[@data-id='dateOfSignSection']");
    public final SelenideElement signedOffParticipationAgreementUploadInput =
            signedOffParticipationAgreementSection.$x(".//input[@type='file']");
    public final LightningDatepicker signedOffDatePicker =
            new LightningDatepicker(signedOffParticipationAgreementSection.$x(".//lightning-datepicker"));
    public final SelenideElement signedOffParticipationAgreementUploadedFilesLink =
            signedOffParticipationAgreementSection.$(".file-details .slds-text-link");

    //  'GST No. & Certificate' section in 'KYC Details' block
    public final SelenideElement gstNumberSection = kycDetailsBlock.$x(".//div[@data-id='gstSection']");
    public final SelenideElement gstStateCodeOfCustomerInput =
            gstNumberSection.$x(".//*[@data-id='state-code']//input");
    public final SelenideElement gstNumberInput = gstNumberSection.$x(".//*[@data-id='gst-no']//input");
    public final SelenideElement gstNumberUploadedFilesLink = gstNumberSection.$(".file-details .slds-text-link");
    public final SelenideElement gstNumberUploadInput = gstNumberSection.$x(".//input[@type='file']");

    //  'Billing Address' section in 'KYC Details' block
    public final SelenideElement billingAddressSection = kycDetailsBlock.$x(".//*[@data-id='billingAddressSection']");
    public final SelenideElement billingStreetInput =
            billingAddressSection.$x(".//*[@data-id='billingStreet']//input");
    public final SelenideElement billingCityInput =
            billingAddressSection.$x(".//*[@data-id='billingCity']//input");
    public final SelenideElement billingStateInput =
            billingAddressSection.$x(".//*[@data-id='billingState']//input");
    public final SelenideElement billingCountryInput =
            billingAddressSection.$x(".//*[@data-id='billingCountry']//input");
    public final SelenideElement billingPostalCodeInput =
            billingAddressSection.$x(".//*[@data-id='billingPostalCode']//input");

    //  'SEZ Exemption Certificate & Number' section in 'KYC Details' block
    public final SelenideElement sezExemptionCertificateSection = kycDetailsBlock.$x(".//div[@data-id='sezSection']");
    public final SelenideElement sezExemptionCertificateUploadInput =
            sezExemptionCertificateSection.$x(".//input[@type='file']");

    //  'PAN/GIR No.' section in 'KYC Details' block
    public final SelenideElement panGirNumberSection = kycDetailsBlock.$x(".//div[@data-id='panGirSection']");
    public final SelenideElement panGirNumberUploadInput = panGirNumberSection.$x(".//input[@type='file']");

    //  'Address of the Authorised Signatory' section in 'KYC Details' block
    public final SelenideElement addressOfTheAuthorisedSignatorySection =
            kycDetailsBlock.$x(".//div[@data-id='signatorySection']");
    public final SelenideElement addressOfTheAuthorisedSignatoryUploadInput =
            addressOfTheAuthorisedSignatorySection.$x(".//input[@type='file']");

    //  'Passport of the Authorised Signatory' section in 'KYC Details' block
    public final SelenideElement passportOfTheAuthorisedSignatorySection =
            kycDetailsBlock.$x(".//div[@data-id='passportSection']");
    public final SelenideElement passportOfTheAuthorisedSignatoryUploadInput =
            passportOfTheAuthorisedSignatorySection.$x(".//input[@type='file']");

    //  'Financial / Tax Details' section in common Details block
    public final SelenideElement gstStateCodeOfCustomerOutput =
            detailsSection.$x(".//*[@field-label='GST State Code of Customer']//*[@data-output-element-id='output-field']");

    //  Modal windows
    public final KycDetailsAttachmentsModal kycDetailsAttachmentsModal = new KycDetailsAttachmentsModal();

    /**
     * Get the formatted error "Provided GST number doesn't match the billing state..."
     * with the expected billing state.
     *
     * @param expectedBillingStateValue billing state that is expected for the provided GST number (e.g. "SK")
     * @return full error message (for the assertion)
     */
    public static String getGstNumberDoesNotMatchBillingStateError(String expectedBillingStateValue) {
        return String.format("%s: %s", GST_NUMBER_DOES_NOT_MATCH_BILLING_STATE_ERROR, expectedBillingStateValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void waitUntilLoaded() {
        super.waitUntilLoaded();
        kycDetailsBlock.shouldBe(visible, ofSeconds(20));
    }

    /**
     * Save applied changes in 'KYC Details' block.
     * <br/>
     * Useful for positive flows when user has provided valid values.
     */
    public void kycDetailsSaveChanges() {
        kycDetailsSaveButton.scrollIntoView("{block: \"center\"}").click();

        kycDetailsSpinner.shouldBe(visible);
        kycDetailsSpinner.shouldBe(hidden, ofSeconds(60));
        notification.shouldNot(exist, ofSeconds(1));
        kycDetailsSaveButton.shouldBe(disabled, ofSeconds(10));
    }
}
