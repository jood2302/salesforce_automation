package com.aquiva.autotests.rc.page.prm;

import com.aquiva.autotests.rc.model.prm.DealRegistrationData;
import com.aquiva.autotests.rc.page.components.LightningCombobox;
import com.aquiva.autotests.rc.page.components.LightningDualListBox;
import com.aquiva.autotests.rc.page.prm.modal.SearchPartnersModal;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$x;
import static java.time.Duration.ofSeconds;

/**
 * The page to create a new Deal Registration record in the PRM Portal.
 */
public class DealRegistrationCreationPage {
    public final LightningCombobox partnerProgramPicklist = new LightningCombobox("Partner Program");

    public final SelenideElement partnerContactSearchButton = $x("//button[span[text()='Please choose a Partner Contact']]");

    public final SelenideElement installationServiceProviderPicklist = $x("//*[@name='Installation_Service_Provider__c']");
    public final SelenideElement firstNameInput = $x("//*[@name='First_Name__c']");
    public final SelenideElement lastNameInput = $x("//*[@name='Last_Name__c']");
    public final SelenideElement companyNameInput = $x("//*[@name='Company_Name__c']");
    public final SelenideElement emailAddressInput = $x("//*[@name='Email_Address__c']");
    public final SelenideElement phoneNumberInput = $x("//*[@name='Phone_Number__c']");

    public final SelenideElement countryPicklist = $x("//*[@name='Country__c']");
    public final SelenideElement statePicklist = $x("//*[@name='State__c']");
    public final SelenideElement cityInput = $x("//*[@name='City__c']");
    public final SelenideElement addressInput = $x("//*[@name='Address__c']");
    public final SelenideElement postalCodeInput = $x("//*[@name='Postal_Code__c']");

    public final SelenideElement forecastedUsersInput = $x("//*[@name='Forecasted_Users__c']");
    public final SelenideElement brandPicklist = $x("//*[@name='brandNames']");
    public final LightningCombobox industryPicklist = new LightningCombobox("Industry");
    public final SelenideElement websiteInput = $x("//*[@name='Website__c']");
    public final SelenideElement numberOfEmployeesPicklist = $x("//*[@name='Number_of_Employees__c']");
    public final SelenideElement estimatedCloseDateInput = $x("//*[@name='Estimated_Close_Date__c']");
    public final SelenideElement howDidYouAcquireThisLeadInput = $x("//*[@name='How_did_you_acquire_this_Lead__c']");
    public final SelenideElement descriptionInput = $x("//*[@name='Description__c']");
    public final LightningCombobox existingSolutionProviderPicklist = new LightningCombobox("Existing Solution Provider");
    public final LightningDualListBox competitorsSelect =
            new LightningDualListBox($x("//*[./*[contains(text(),'Competitors')]]//following-sibling::*[1]//lightning-dual-listbox/*"));
    public final LightningDualListBox whatsPromptingChangeSelect =
            new LightningDualListBox($x("//*[./span[contains(text(),'prompting change')]]//following-sibling::*[1]//lightning-dual-listbox/*"));

    public final LightningCombobox isThisAnExistingMitelCustomerPicklist =
            new LightningCombobox("Is this an existing Mitel Customer?");
    public final SelenideElement cloudSpecialistInput =
            $x("//div[span[contains(text(), 'Cloud Specialist')]]/following-sibling::div//input");

    public final SelenideElement submitButton = $x("//button[@type='submit']");

    public final SelenideElement spinner = $x("//lightning-spinner[@class='slds-spinner_container']");

    public final SearchPartnersModal searchPartnersModal = new SearchPartnersModal();

    /**
     * Select necessary value in 'Partner Program' picklist
     * populate all the required fields on the form and save it.
     *
     * @param data object with all the test data for all the fields to populate
     */
    public void submitFormWithPartnerProgram(DealRegistrationData data) {
        selectPartnerProgram(data.partnerProgram);
        populateFormWithTestData(data);
        populateIgnitePortalSpecificFields(data);
        submitButton.click();
    }

    /**
     * Select Partner Contact in the search field,
     * populate the required fields and submit the form.
     * The Partner Contact is selected from the list of Contacts.
     *
     * @param data               object with all the test data for all the fields to populate
     * @param partnerContactName the Partner Contact to select from the list
     */
    public void submitFormWithPartnerContactSearch(DealRegistrationData data, String partnerContactName) {
        selectPartnerContact(partnerContactName);
        populateFormWithTestData(data);
        submitButton.click();
    }

    /**
     * Select necessary value in 'Partner Program' picklist,
     * select Partner Contact in the search field,
     * populate the required fields and submit the form.
     * The Partner Contact is selected from the list of Contacts.
     *
     * @param data               object with all the test data for all the fields to populate
     * @param partnerContactName the Partner Contact to select from the list
     */
    public void submitFormWithPartnerProgramAndPartnerContactSearch(DealRegistrationData data, String partnerContactName) {
        selectPartnerProgram(data.partnerProgram);
        selectPartnerContact(partnerContactName);
        populateFormWithTestData(data);
        populateIgnitePortalSpecificFields(data);
        submitButton.click();
    }

    /**
     * Select Partner Contact via the search function.
     * <br/>
     * Selects the first found Partner Contact from the search results.
     *
     * @param partnerContactName name of the Partner Contact
     */
    private void selectPartnerContact(String partnerContactName) {
        partnerContactSearchButton.shouldBe(enabled, ofSeconds(30)).click();
        searchPartnersModal.contactSearchButton.shouldBe(visible, ofSeconds(30));
        searchPartnersModal.contactFullNameSearchInput.setValue(partnerContactName);
        searchPartnersModal.contactSearchButton.click();
        searchPartnersModal.contactSearchFirstResult.shouldBe(visible, ofSeconds(10)).click();
    }

    /**
     * Select a value for Partner Program.
     * <br/>
     * Applicable for the Ignite Portal.
     *
     * @param partnerProgram name of the Partner Program (e.g. "Channel Harmony")
     */
    private void selectPartnerProgram(String partnerProgram) {
        partnerProgramPicklist.getInput().shouldBe(visible, ofSeconds(10));
        partnerProgramPicklist.selectOption(partnerProgram);
    }

    /**
     * Populate all the required fields on the form with the provided test data.
     * This method is used to fill the form with the test data before submitting it.
     *
     * @param data object with all the test data for all the fields to populate
     */
    private void populateFormWithTestData(DealRegistrationData data) {
        firstNameInput.setValue(data.firstName);
        lastNameInput.setValue(data.lastName);
        companyNameInput.setValue(data.companyName);
        emailAddressInput.setValue(data.emailAddress);
        phoneNumberInput.setValue(data.phoneNumber);

        countryPicklist.selectOption(data.country);
        statePicklist.selectOption(data.state);
        cityInput.setValue(data.city);
        addressInput.setValue(data.address);
        postalCodeInput.setValue(data.postalCode);

        forecastedUsersInput.setValue(data.forecastedUsers);
        brandPicklist.selectOption(data.brand);

        industryPicklist.selectOption(data.industry);
        websiteInput.setValue(data.website);
        numberOfEmployeesPicklist.selectOption(data.numberOfEmployees);
        estimatedCloseDateInput.setValue(data.getEstimatedCloseDateFormatted());
        howDidYouAcquireThisLeadInput.setValue(data.howDidYouAcquireThisLead);
        descriptionInput.setValue(data.description);
        existingSolutionProviderPicklist.selectOption(data.existingSolutionProvider);
        competitorsSelect.selectOptions(data.competitors);
        whatsPromptingChangeSelect.selectOptions(data.whatsPromptingChange);
    }

    /**
     * Populate some necessary fields that are specific to the Deal Registrations
     * on the Ignite Portal.
     *
     * @param data object with all the test data for all the fields to populate
     */
    private void populateIgnitePortalSpecificFields(DealRegistrationData data) {
        isThisAnExistingMitelCustomerPicklist.selectOption(data.isThisAnExistingMitelCustomer);
        installationServiceProviderPicklist.selectOption(data.installationServiceProvider);
    }
}
