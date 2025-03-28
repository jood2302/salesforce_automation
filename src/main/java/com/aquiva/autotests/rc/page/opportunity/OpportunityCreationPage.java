package com.aquiva.autotests.rc.page.opportunity;

import com.aquiva.autotests.rc.page.components.Calendar;
import com.aquiva.autotests.rc.page.components.ShippingAddressForm;
import com.aquiva.autotests.rc.page.components.lookup.AngularLookupComponent;
import com.aquiva.autotests.rc.page.salesforce.VisualforcePage;
import com.aquiva.autotests.rc.page.salesforce.account.AccountRecordPage;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.aquiva.autotests.rc.utilities.Constants.BASE_VF_URL;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.*;
import static java.time.Duration.ofSeconds;

/**
 * Quick Opportunity Creation Page.
 * Opens after:
 * <p> 1. Clicking 'Opportunities' -> 'New' and selecting 'New Sales Opportunity'
 * type in {@link OpportunityRecordTypeSelectionModal} window</p>
 * <p> 2. Clicking on {@link AccountRecordPage} 'Opportunities' -> 'New' and selecting 'New Sales Opportunity'
 * type in {@link OpportunityRecordTypeSelectionModal} window</p>
 * <br/>
 * Sales users use this page to create new Opportunities.
 * On this page, they can select account and its contact, set number of digital lines.
 * <br/>
 * When everything's set up here, user is transferred to {@link OpportunityRecordPage}
 * for this new Opportunity object.
 * <br/>
 */
public class OpportunityCreationPage extends VisualforcePage {

    /**
     * Direct link to Opportunity Creation page.
     */
    private static final String OPPORTUNITY_CREATION_PAGE_URL = BASE_VF_URL +
            "/apex/OpportunityCreationForm" +
            "?retURL=%2F006%2Fo" +
            "&RecordType=01234000000HpTN" +
            "&ent=Opportunity" +
            "&save_new=1" +
            "&sfdc.override=1";

    //  Field labels
    public static final String NEW_NUMBER_OF_USERS = "New Number of Users";
    public static final String NEW_NUMBER_OF_LICENSES = "New Number of Licenses";

    //  Error and info messages
    public static final String CLOSE_DATE_IS_REQUIRED_ERROR = "Close Date is required";
    public static final String CLOSE_DATE_IN_THE_PAST_ERROR = "Close Date can't be in the Past";
    public static final String PROVISIONING_DETAILS_ARE_REQUIRED_FOR_DOWNSELL_ERROR =
            "Provisioning Details are required for Downsell";
    public static final String NO_PRIMARY_OR_SIGNATORY_CONTACT_ON_ACCOUNT = "You can't proceed with Opportunity Creation. " +
            "There are no Primary or Signatory Contacts under selected Account.";
    public static final String NO_ACCOUNT_SELECTED_MESSAGE = "No account selected";

    //  'Enter Opportunity Info' section
    public final SelenideElement opportunityInfoSection = $x("//li[.//opportunity-info]");
    public final AngularLookupComponent accountComboboxInput = new AngularLookupComponent();
    public final SelenideElement accountInputWithSelectedValue =
            $("[data-ui-auto='defaultAccountLookupCombobox'] .selected-value");
    public final AngularLookupComponent contactLookupInput = new AngularLookupComponent(
            $("[data-ui-auto='defaultContactLookupCombobox']"));
    public final SelenideElement contactInputWithSelectedValue =
            $("[data-ui-auto='defaultContactLookupCombobox'] .selected-value");
    public final SelenideElement opportunityNameInput = $("[formcontrolname='oppName']");
    public final SelenideElement closeDateSection = $("#closed-date");
    public final SelenideElement closeDateTextInput = $("[data-ui-auto='close-date']");
    public final SelenideElement provisioningDetailsTextArea = $("[data-ui-auto='provisioning-details']");
    public final SelenideElement provisioningDetailsError = provisioningDetailsTextArea.parent().sibling(0);
    public final SelenideElement shippingAddressBox = $("[data-ui-auto='shipping-address']");
    public final SelenideElement notificationBarErrorIcon = $("notification-bar .slds-theme_error");
    public final SelenideElement successNotificationIcon = $("notifications .slds-theme_success");
    public final ElementsCollection notificationBarNotifications = $$("notification-bar p.notification__text");
    public final SelenideElement successNotification = $("notifications h2.notification-text");
    public final Calendar calendar = new Calendar(); // for 'Close Date' input
    public final ShippingAddressForm shippingAddressForm = new ShippingAddressForm();

    //  'Select Service Plan' section
    public final SelenideElement servicePlanSection = $x("//li[.//service-plan-selector]");
    public final SelenideElement billingSystem = servicePlanSection.$(byText("Billing System")).sibling(0);
    public final SelenideElement businessIdentityPicklist = $("#select-business-identity-filter");
    public final SelenideElement brandOutput = servicePlanSection.$(byText("Brand")).sibling(0);

    public final SelenideElement currencyPicklist = $("#select-currency-filter");
    public final SelenideElement servicePicklist = $("#select-service-filter");
    public final SelenideElement existingNumberOfDLsOutput = $("input[formcontrolname='existingDLs']");
    public final SelenideElement newNumberOfDLsInput = $("[datauiauto='new-number-of-dls'] input");
    public final SelenideElement newNumberOfDLsLabel = $("[datauiauto='new-number-of-dls'] label");

    //  Other page elements
    public final SelenideElement continueToOppButton = $("#create-opportunity-action");
    public final SelenideElement spinner = $("[data-ui-auto='spinner']");

    /**
     * Constructor that defines Quick Opportunity page's location
     * using its iframe's title.
     */
    public OpportunityCreationPage() {
        super("New Opportunity");
    }

    /**
     * Open "Quick Opportunity Page" by direct link.
     * <br/>
     * It allows skipping:
     * <p> 1. Opening corresponding account record </p>
     * <p> 2. Pressing "New" in Opportunity section </p>
     * <p> 3. Selecting "New Sales Opportunity" there </p>
     * <p></p>
     *
     * @param accountId ID of Account for which Opportunity is created
     * @return opened NGBS Opportunity Creation Page reference
     */
    public OpportunityCreationPage openPage(String accountId) {
        open(OPPORTUNITY_CREATION_PAGE_URL + "&accid=" + accountId);

        accountInputWithSelectedValue.shouldBe(visible, ofSeconds(60));
        return this;
    }

    /**
     * Populate 'Close Date' field with today's date.
     */
    public void populateCloseDate() {
        closeDateTextInput.shouldBe(visible, ofSeconds(30)).click();
        calendar.setTodayDate();
    }

    /**
     * <p> 1. Open Shipping Address form. </p>
     * <p> 2. Set Customer Name. </p>
     * <p> 3. Select Shipping Option. </p>
     * <p> 4. Click 'Apply' button. </p>
     */
    public void applyShippingDetails(String customerName, String shippingOption) {
        shippingAddressBox.click();
        shippingAddressForm.getCustomerName().setValue(customerName);
        shippingAddressForm.getShippingOptionPicklist().selectOption(shippingOption);
        shippingAddressForm.applyShippingForm();
    }
}
