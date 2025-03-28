package com.aquiva.autotests.rc.page.lead;

import com.aquiva.autotests.rc.page.components.LightningCombobox;
import com.aquiva.autotests.rc.page.salesforce.VisualforcePage;
import com.codeborne.selenide.SelenideElement;
import com.sforce.soap.enterprise.sobject.Lead;

import static com.aquiva.autotests.rc.utilities.Constants.BASE_VF_URL;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.*;
import static java.time.Duration.ofSeconds;

/**
 * The Visualforce page that displays elements necessary fields to create a new ({@link Lead}) record.
 */
public class LeadCreationPage extends VisualforcePage {

    public final SelenideElement firstNameInput = $x("//*[@name='firstName']");
    public final SelenideElement lastNameInput = $x("//*[@name='lName']");
    public final SelenideElement companyInput = $x("//*[@name='company']");
    public final SelenideElement emailInput = $x("//*[@name='email']");
    public final SelenideElement contactPhoneNumberInput = $x("//*[@name='contactPhoneNumber']");
    public final LightningCombobox leadSourcePicklist = new LightningCombobox("Lead Source");
    public final LightningCombobox numberOfEmployeesPicklist = new LightningCombobox("No. of Employees (Range)");
    public final LightningCombobox industryPicklist = new LightningCombobox("Industry");
    public final SelenideElement websiteInput = $x("//*[@name='website']");

    //  Buttons
    private final SelenideElement createNewLeadButton = $x("//button[@title='Create New Lead']");
    private final SelenideElement searchButton = $x("//button[@title='Search']");

    //  Error notifications
    private final SelenideElement errorNotificationContainer = $x("//div[contains(@class, 'slds-notify_container')]");

    //  Spinner
    private final SelenideElement spinner = $x("//lightning-spinner[@class='slds-spinner_container']");

    /**
     * Constructor that defines Lead Creation page's location
     * using its default web element.
     */
    public LeadCreationPage() {
        super($("c-smart-search-lwc-container"));
    }

    /**
     * Open the page with the form to create a new Lead record via direct link.
     */
    public void openPage() {
        open(BASE_VF_URL + "/apex/LeadSearchExtension");
        createNewLeadButton.shouldBe(visible, ofSeconds(60));
    }

    /**
     * Click 'Search' button in order to search for duplicate records
     * after filling the form before new Lead record creation.
     */
    public void searchValidInputs() {
        searchButton.click();
        spinner.shouldBe(visible);
        spinner.shouldBe(hidden, ofSeconds(10));
        errorNotificationContainer.shouldNotBe(visible);
    }

    /**
     * Click 'Create New Lead' button to create a new Lead record.
     */
    public void clickCreateNewLeadButton() {
        createNewLeadButton.click();
        spinner.shouldBe(visible);
        spinner.shouldBe(hidden, ofSeconds(30));
    }
}
