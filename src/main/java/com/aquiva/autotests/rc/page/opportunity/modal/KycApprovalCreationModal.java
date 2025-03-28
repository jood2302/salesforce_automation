package com.aquiva.autotests.rc.page.opportunity.modal;

import com.aquiva.autotests.rc.page.components.LightningCombobox;
import com.aquiva.autotests.rc.page.opportunity.OpportunityRecordPage;
import com.aquiva.autotests.rc.page.salesforce.account.AccountRecordPage;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.sforce.soap.enterprise.sobject.Approval__c;

import java.time.format.DateTimeFormatter;

import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.KYC_APPROVAL_RECORD_TYPE;

/**
 * {@link Approval__c} creation modal of 'KYC Approval Request' approval type.
 * <br/>
 * Contains some type-related fields, like 'RC Point of Presence', purposes for procuring the Service,
 * 'Type of Customer' etc.
 * <p>
 * <b> Note: Might be opened from {@link AccountRecordPage} or {@link OpportunityRecordPage} pages. </b>
 */
public class KycApprovalCreationModal extends ApprovalCreationModal {

    public static final DateTimeFormatter KYC_APPROVAL_DATES_FORMATTER = DateTimeFormatter.ofPattern("M/d/yyyy");

    //  'Telecom Circle Details' section
    public final SelenideElement rcPointOfPresenceInput = dialogContainer.$x(".//input[@name='RcPointOfPresence__c']");

    //  'Financial / Tax Details' section
    public final SelenideElement panGirNumberInput = dialogContainer.$x(".//input[@name='PanGirNo__c']");
    public final SelenideElement gstNumberOutput = dialogContainer.$x(".//span[contains(text(),'GST No.')]" +
                    "//ancestor::dl//lightning-formatted-text");
    public final SelenideElement gstStateCodeOutput = dialogContainer.$x(".//span[contains(text(),'GST State Code of Customer')]" +
            "/ancestor::dt/following-sibling::dd//lightning-formatted-text");
    public final SelenideElement gstStateNameOutput = dialogContainer.$x(".//span[contains(text(),'GST State Name of Customer')]" +
            "/ancestor::dt/following-sibling::dd//lightning-formatted-text");
    public final SelenideElement gstExemptionSezCustomerInput = dialogContainer.$x(".//input[@name='GstExemptionSezCustomer__c']");
    public final LightningCombobox typeOfCustomerInput = new LightningCombobox("Type of Customer");
    public final LightningCombobox gstTypeInput = new LightningCombobox("GST - Regular or Composite");

    //  'Connection / Service Details' section
    public final ElementsCollection chosenPurposesForProcuringServicesList = dialogContainer
            .$$x(".//span[contains(text(), 'Chosen')]/following-sibling::div//ul/li");
    public final SelenideElement otherTextarea = dialogContainer
            .$x(".//label[contains(text(), 'Other')]/following-sibling::div/textarea");

    //  'Authorised Signatory Details' section
    public final SelenideElement nameOfTheAuthorisedSignatoryInput = dialogContainer
            .$x(".//input[@name='NameOfTheAuthorisedSignatory__c']");
    public final SelenideElement nameOfFatherOrHusbandInput = dialogContainer
            .$x(".//input[@name='NameOfFatherOrHusband__c']");
    public final LightningCombobox genderOfAuthorisedSignatoryInput =
            new LightningCombobox("Gender of Authorised Signatory");
    public final SelenideElement dateOfBirthOfAuthorisedSignatoryInput = dialogContainer
            .$x(".//input[@name='DateOfBirthOfAuthorisedSignatory__c']");
    public final SelenideElement nationalityOfAuthorisedSignatoryOutput = dialogContainer
            .$x(".//*[@field-label='Nationality of Authorised Signatory']//*[@data-output-element-id='output-field']");
    public final SelenideElement coDoSoWoHoInput = dialogContainer.$x(".//input[@name='CoDoSoWoHo__c']");
    public final SelenideElement houseNoInput = dialogContainer.$x(".//input[@name='HouseNo__c']");
    public final SelenideElement streetAddressInput = dialogContainer.$x(".//input[@name='StreetAddress__c']");
    public final SelenideElement landmarkInput = dialogContainer.$x(".//input[@name='Landmark__c']");
    public final SelenideElement areaSectorLocalityInput = dialogContainer.$x(".//input[@name='Area__c']");
    public final SelenideElement villageTownCityInput = dialogContainer.$x(".//input[@name='City__c']");
    public final SelenideElement districtInput = dialogContainer.$x(".//input[@name='District__c']");
    public final LightningCombobox stateUtInput = new LightningCombobox("State/UT");
    public final SelenideElement pinCodeInput = dialogContainer.$x(".//input[@name='PinCode__c']");

    /**
     * Constructor for the modal window to locate it via its default header.
     */
    public KycApprovalCreationModal() {
        super(KYC_APPROVAL_RECORD_TYPE);
    }
}
