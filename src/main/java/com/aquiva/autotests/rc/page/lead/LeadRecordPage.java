package com.aquiva.autotests.rc.page.lead;

import com.aquiva.autotests.rc.page.components.LightningCombobox;
import com.aquiva.autotests.rc.page.salesforce.RecordPage;
import com.codeborne.selenide.SelenideElement;
import com.sforce.soap.enterprise.sobject.Lead;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThanOrEqual;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$x;
import static java.time.Duration.ofSeconds;
import static java.util.UUID.randomUUID;

/**
 * The Standard Salesforce page that displays Lead ({@link Lead}) record information.
 */
public class LeadRecordPage extends RecordPage {

    //  Default address values
    private static final String DEFAULT_US_STREET = "123 Main St";
    private static final String DEFAULT_US_CITY = "Belmont";
    private static final String DEFAULT_US_STATE_PROVINCE = "CA";
    private static final String DEFAULT_US_ZIP_POSTAL_CODE = "94002";
    private static final String DEFAULT_US_COUNTRY = "United States";

    public final SelenideElement heading = $x("//h1[div//text()='Lead']");
    public final SelenideElement editModalHeading = $x("//h2[contains(text(),'Edit')]");

    //  Lead Qualification section
    private final SelenideElement leadQualificationCurrentSituationInput =
            $x("//*[./*[text()='Lead Qualification Current Situation']]//textarea");
    private final SelenideElement leadQualificationNextStepsInput =
            $x("//*[./*[text()='Lead Qualification Next Steps']]//textarea");
    private final SelenideElement leadQualificationProblemsInput =
            $x("//*[./*[text()='Lead Qualification Problems']]//textarea");
    private final SelenideElement decisionMakingProcessInput =
            $x("//*[./*[text()='Decision Making Process']]//textarea");
    private final SelenideElement descriptionInput =
            $x("//*[./*[text()='Description']]//textarea");

    //  Details tab, address fields
    private final SelenideElement addressStreetInput = $x("//textarea[@name='street']");
    private final SelenideElement addressCityInput = $x("//input[@name='city']");
    private final SelenideElement addressStateProvinceInput = $x("//input[@name='province']");
    private final SelenideElement addressZipPostalCodeInput = $x("//input[@name='postalCode']");
    private final SelenideElement addressCountryInput = $x("//input[@name='country']");

    //  Lead Information section
    private final LightningCombobox addressCountryPicklist = new LightningCombobox("Country");

    /**
     * {@inheritDoc}
     */
    public void waitUntilLoaded() {
        detailsTab.shouldBe(visible, ofSeconds(100));
        visibleLightingActionButtons.shouldHave(sizeGreaterThanOrEqual(3), ofSeconds(100));
        heading.shouldBe(visible);
    }

    /**
     * Press "Convert"/"Convert Lead" button on the Lead record page.
     * <br/><br/>
     * This method searches the button among Lightning Experience actions
     * in the upper right corner of the page
     * (even if the button is hidden in the "show more actions" list).
     */
    public void clickConvertButton() {
        clickDetailPageButton("Convert");
    }

    /**
     * Press "Create a Case" button on the Lead record page.
     * <br/><br/>
     * This method searches the button among Lightning Experience actions
     * in the upper right corner of the page
     * (even if the button is hidden in the "show more actions" list).
     */
    public void clickCreateCaseButton() {
        clickDetailPageButton("Create a Case");
    }

    /**
     * Press "Edit" button on the Lead record page.
     * <br/><br/>
     * This method searches the button among Lightning Experience actions
     * in the upper right corner of the page
     * (even if the button is hidden in the "show more actions" list).
     */
    public void clickEditButton() {
        clickDetailPageButton("Edit");
        editModalHeading.shouldBe(visible, ofSeconds(20));
    }

    /**
     * Populate all the fields related to Lead Qualification with some default values.
     */
    public void populateLeadQualificationFields() {
        leadQualificationCurrentSituationInput.setValue("Test Current Situation " + randomUUID());
        leadQualificationNextStepsInput.setValue("Test Next Steps " + randomUUID());
        leadQualificationProblemsInput.setValue("Test Problems " + randomUUID());
        decisionMakingProcessInput.setValue("Test Making Process " + randomUUID());
        descriptionInput.setValue("Test Description " + randomUUID());
    }

    /**
     * Click "Edit" button on the Lead record page,
     * populate all the address fields with some default US values.
     */
    public void populateAddressFields() {
        populateAddressFields(DEFAULT_US_COUNTRY, DEFAULT_US_STATE_PROVINCE, DEFAULT_US_CITY,
                DEFAULT_US_STREET, DEFAULT_US_ZIP_POSTAL_CODE);
    }

    /**
     * Populate all the address-related fields.
     *
     * @param country       the country name (e.g. "United States", "Canada", "United Kingdom", etc.)
     * @param stateProvince the state or province name (e.g. "California", "Ontario", "England", etc.)
     * @param city          the city name (e.g. "Belmont", "Toronto", "London", etc.)
     * @param street        the street name (e.g. "123 Main St", "456 Elm St", "789 Oak St", etc.)
     * @param zipPostalCode the ZIP or postal code (e.g. "94002", "M5V 1A1", "SW1A 1AA", etc.)
     */
    public void populateAddressFields(String country, String stateProvince, String city, String street, String zipPostalCode) {
        addressCountryInput.setValue(country);
        addressCountryPicklist.selectOption(country);
        addressStateProvinceInput.setValue(stateProvince);
        addressCityInput.setValue(city);
        addressStreetInput.setValue(street);
        addressZipPostalCodeInput.setValue(zipPostalCode);
    }
}
