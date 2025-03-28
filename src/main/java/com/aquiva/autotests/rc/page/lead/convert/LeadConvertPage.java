package com.aquiva.autotests.rc.page.lead.convert;

import com.aquiva.autotests.rc.page.components.LightningCombobox;
import com.aquiva.autotests.rc.page.components.LightningDatepicker;
import com.aquiva.autotests.rc.page.components.lookup.CustomLwcLookupComponent;
import com.aquiva.autotests.rc.page.salesforce.VisualforcePage;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import java.util.List;

import static com.aquiva.autotests.rc.utilities.Constants.BASE_VF_URL;
import static com.codeborne.selenide.CollectionCondition.itemWithText;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selectors.withText;
import static com.codeborne.selenide.Selenide.*;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;

/**
 * Visualforce page for Lead Conversion.
 * Contains Account, Opportunity, Contact, Opportunity Role and Lead Qualification sections.
 * <br/>
 * <p> - Account section is used for selecting existing Account or creating new.</p>
 * <p> - Opportunity section is used for selecting package.</p>
 * <p> - Contact section is used for selecting matching Contact.</p>
 * <p> - Opportunity Role section is used for selecting Role (e.g. "Influencer", "Decision Maker" etc.).</p>
 * <p> - Lead Qualification section is used for setting qualification if available (e.g. "STC-123456", "LQ-654321").</p>
 */
public class LeadConvertPage extends VisualforcePage {

    /**
     * Direct link to Lead Convert page.
     */
    public static final String LEAD_CONVERT_PAGE_URL = BASE_VF_URL + "/apex/ConvertLead";
    
    public static final String LOADING_SERVICE = "Loading...";

    public static final String NGBS_BILLING_SYSTEM = "NGBS";
    public static final String LEGACY_BILLING_SYSTEM = "Legacy";

    public static final String TYPE_NEW_BUSINESS = "Type: New business";

    public static final String NONE_BUSINESS_IDENTITY = "--None--";

    public static final String ACCOUNT_IS_REQUIRED_ERROR = "Account is required\n" +
            "Please specify an existing account or create a new one";
    public static final String SELECT_CLOSE_DATE_IS_REQUIRED_ERROR = "Close Date is required\n" +
            "Select Close Date";
    public static final String CLOSE_DATE_IS_REQUIRED_ERROR = "Close Date is required";
    public static final String YOUR_ENTRY_DOESNT_MATCH_ALLOWED_FORMAT_ERROR =
            "Your entry does not match the allowed format";
    public static final String CLOSE_DATE_CANNOT_BE_IN_THE_PAST_ERROR = "Close Date can't be in the past";

    public static final String NEW_ACCOUNT_WILL_BE_CREATED_MESSAGE = "New account will be created";
    public static final String NO_CONTACTS_MATCH_THE_CHOSEN_ACCOUNT_MESSAGE = "No Contacts match the chosen Account\n" +
            "A new Contact will be created.";
    public static final String NO_OPPORTUNITY_WILL_BE_CREATED_MESSAGE = "No Opportunity will be created";
    public static final String NO_OPPORTUNITIES_MATCH_CHOSEN_ACCOUNT_MESSAGE = "No Opportunities match the chosen Account. " +
            "A new Opportunity will be created.";
    public static final String ACCOUNT_RECORD_IS_CREATED_MESSAGE = "Account record is created.";
    public static final String CONTACT_RECORD_IS_CREATED_MESSAGE = "Contact record is created.";
    public static final String OPPORTUNITY_RECORD_IS_CREATED_MESSAGE = "Opportunity record is created.";
    public static final String NO_MATCHING_BUSINESS_IDENTITY_WAS_FOUND = "No matching Business Identity was found for the specified Country and Brand. " +
            "If you are certain that the lead's Country and Brand information is accurate, " +
            "kindly create a case for further assistance.";

    public static final String START_TYPING_TO_SEARCH_ACCOUNT_PLACEHOLDER = "Start typing to search Account";
    public static final String MATCHED_ACCOUNTS_TABLE_LABEL = "Matched Accounts";
    public static final List<String> MATCHED_ACCOUNTS_HEADERS = List.of(
            "Account Name", "RC User ID", "RC Account Number",
            "RC Account Status", "Type", "Current Owner", "Last Modified Date"
    );
    public static final String MATCHED_ACCOUNT_NB_TYPE = "New business";
    public static final String MATCHED_ACCOUNT_EB_NGBS_TYPE = "Existing business (NGBS)";

    public static final String MATCHED_OPPORTUNITIES_TABLE_LABEL = "Choose from matched opportunities";
    public static final List<String> MATCHED_OPPORTUNITIES_HEADERS = List.of(
            "Opportunity Name", "Stage", "Opportunity Owner",
            "Estimated 12 Month Booking", "Last Modified Date", "Opportunity Record Type"
    );

    public static final String MATCHED_CONTACTS_TABLE_LABEL =
            "Choose from matched Contacts from the chosen Account";
    public static final String SELECTED_CONTACT_ALREADY_HAS_A_ROLE_MESSAGE =
            "Selected Contact already has a Role in the selected Opportunity";
    public static final List<String> MATCHED_CONTACTS_HEADERS = List.of(
            "Contact Name", "Company", "Title",
            "Email", "Phone", "Account Status", "Last Modified Date"
    );

    public static final String INFLUENCER_OPPORTUNITY_CONTACT_ROLE = "Influencer";
    public static final String DECISION_MAKER_OPPORTUNITY_CONTACT_ROLE = "Decision Maker";

    //  Elements
    public final SelenideElement createCaseButton = $x("//button[text()='Create a Case']");
    public final SelenideElement spinner = $x("//div[@c-lwcspinner_lwcspinner]");
    public final SelenideElement convertButton = $x("//button[text()='Convert']");
    public final SelenideElement convertButtonTooltip = convertButton.$("[role='tooltip']");
    public final ElementsCollection convertedRecords = $$x("//div[@class='slds-setup-assistant__step-summary']//p");

    //  Account Info section
    public final SelenideElement accountInfoSection = $x("//c-cl-account-selector/li");
    public final SelenideElement accountInfoLabel = accountInfoSection.$("p");
    public final CustomLwcLookupComponent existingAccountSearchInput =
            new CustomLwcLookupComponent(accountInfoSection.$x(".//c-input-lookup/div"));
    public final SelenideElement newExistingAccountToggle = accountInfoSection.$x(".//lightning-input[.//*[@name='createNewAccount']]");
    public final SelenideElement newAccountName = accountInfoSection.$("[class*='summary-detail'] h3");
    public final SelenideElement newAccountType = accountInfoSection.$("[class*='summary-detail'] ul > li");
    public final SelenideElement elaCheckbox = accountInfoSection.$x(".//*[@name='isElaAccount']").parent();
    public final SelenideElement elaServiceAccountsNumberInput = accountInfoSection.$x(".//*[@name='numberOfELAServiceAccounts']");
    public final SelenideElement accountInfoEditButton = accountInfoSection.$(byText("Edit"));
    public final SelenideElement accountInfoApplyButton = accountInfoSection.$(byText("Apply"));
    public final SelenideElement errorMessageOnAccountSelection = accountInfoSection.$(".slds-text-color_error");

    //  Account Info section ('Matched Accounts')
    private final SelenideElement matchedAccountsTable = accountInfoSection.$(".table-container");
    public final SelenideElement matchedAccountsTableLabel = matchedAccountsTable.preceding(0);
    public final ElementsCollection matchedAccountsTableItems = matchedAccountsTable.$$x(".//tbody/tr");
    public final ElementsCollection matchedAccountsTableRadioButtons = matchedAccountsTable.$$x(".//tbody/tr//input");
    public final ElementsCollection matchedAccountsTableHeaders = matchedAccountsTable.$$("thead [title]");

    //  Opportunity Info section
    public final SelenideElement opportunityInfoSection = $x("//div").$(withText("Opportunity")).ancestor("li");
    public final SelenideElement opportunityLoadingBar = opportunityInfoSection.$x(".//c-placeholder-loading");
    public final SelenideElement opportunityInfoLabel = opportunityInfoSection.$x(".//label[@c-clopportunityinfo_salesleadtemplate]");
    public final SelenideElement opportunityInfoEditButton = opportunityInfoSection.$(byText("Edit"));
    public final SelenideElement opportunityInfoApplyButton = opportunityInfoSection.$(byText("Apply"));
    public final SelenideElement opportunityNameInput = opportunityInfoSection.$x(".//input[@name='opportunityName']");
    public final LightningDatepicker closeDateDatepicker =
            new LightningDatepicker(opportunityInfoSection.$x(".//*[@data-id='closeDate']//lightning-datepicker"));
    public final SelenideElement opportunityCreateNewOppOption =
            opportunityInfoSection.$(byText("Create new Opportunity"));
    public final SelenideElement opportunityCreateNewOppOptionInput =
            opportunityCreateNewOppOption.ancestor("*/input");
    public final SelenideElement opportunityDoNotCreateOppOption =
            opportunityInfoSection.$(byText("Do not create Opportunity"));
    public final SelenideElement opportunityDoNotCreateOppOptionInput =
            opportunityDoNotCreateOppOption.ancestor("*/input");
    public final SelenideElement opportunitySelectExistingOppOption =
            opportunityInfoSection.$(byText("Select existing Opportunity"));
    public final SelenideElement opportunitySelectExistingOppOptionInput =
            opportunitySelectExistingOppOption.ancestor("*/input");
    public final SelenideElement tryingToConnectWithNgbsError =
            opportunityInfoSection.$(byText("Trying to connect with NGBS..."));
    public final ElementsCollection opportunityInfoErrorMessages =
            opportunityInfoSection.$$x(".//ul[contains(@class, 'slds-text-color_error')]/li");

    //  Opportunity Info section ('Matched Opportunities')
    private final SelenideElement matchedOpportunitiesTable = opportunityInfoSection.$(".slds-summary-detail__content");
    public final SelenideElement matchedOpportunitiesTableLabel = matchedOpportunitiesTable.$("h4");
    public final ElementsCollection matchedOpportunitiesTableItems = matchedOpportunitiesTable.$$x(".//tbody/tr");
    public final ElementsCollection matchedOpportunitiesTableRadioButtons = matchedOpportunitiesTable.$$x(".//tbody/tr//input");
    public final ElementsCollection matchedOpportunitiesTableHeaders = matchedOpportunitiesTable.$$("thead [title]");

    //  Opportunity Info section ('Select service plan')
    public final SelenideElement businessIdentityPicklist = $x("//div[label[text()='Business Identity']]//select");
    public final SelenideElement currencyPicklist = $x("//div[label[text()='Currency']]//select");
    public final SelenideElement countryPicklist = $x("//div[label[text()='Country']]//select");
    /**
     * @see #selectService(String)
     */
    public final SelenideElement servicePickList = $x("//div[label[text()='Service']]//select");
    public final SelenideElement forecastedUsersInput = $x("//input[@name='forecastedUsers']");
    public final SelenideElement forecastedContactCenterUsersInput = $x("//input[@name='forecastedCCUsers']");
    public final SelenideElement forecastedEngageVoiceUsersInput = $x("//input[@name='forecastedVoiceUsers']");
    public final SelenideElement forecastedEngageDigitalUsersInput = $x("//input[@name='forecastedDigitalUsers']");
    public final SelenideElement forecastedGlobalOfficeUsersInput = $x("//input[@name='forecastedGlobalUsers']");
    public final SelenideElement forecastedRCVideoUsersInput = $x("//input[@name='forecastedRCVideoUsers']");
    public final SelenideElement billingSystemOutputField = $x("//p[text()='Billing System:']/following-sibling::p");
    public final SelenideElement brandNameOutputField = $x("//p[text()='Brand']/following-sibling::p");

    //  Opportunity Info section (in display/non-edit mode)
    public final SelenideElement opportunityNameNonEditable =
            $x("//div[./p='Opportunity Name']/h3");
    public final SelenideElement opportunityBusinessIdentityNonEditable =
            $x("//div[./p='Business Identity']/h3");
    public final SelenideElement opportunityBrandNonEditable =
            $x("//div[./p='Brand']/h3");
    public final SelenideElement opportunityCurrencyNonEditable =
            $x("//div[./p='Currency']/h3");
    public final SelenideElement opportunityCountryNonEditable =
            $x("//div[./p='Country']/h3");

    //  Contact Info section
    public final SelenideElement contactInfoSection = $x("//c-cl-contact-selector/li");
    public final SelenideElement contactDetailsInfoSection = contactInfoSection.$(".slds-summary-detail__content");
    public final SelenideElement contactInfoApplyButton = contactInfoSection.$(byText("Apply"));
    public final SelenideElement contactInfoSelectedContactName = contactInfoSection.$x(".//*[./p='Selected contact']/div");
    public final SelenideElement contactInfoSelectedContactFullName = contactInfoSection.$x(".//h3/a");

    //  Contact Info section ('Matched Contacts')
    private final SelenideElement matchedContactsTable = contactInfoSection.$(".slds-summary-detail__content");
    public final SelenideElement matchedContactsTableLabel = matchedContactsTable.$("h4");
    public final ElementsCollection matchedContactsTableItems = matchedContactsTable.$$x(".//tbody/tr");
    public final ElementsCollection matchedContactsTableRadioButtons = matchedContactsTable.$$x(".//tbody/tr//input");
    public final ElementsCollection matchedContactsTableHeaders = matchedContactsTable.$$("thead [title]");

    //  Opportunity Role Section
    public final SelenideElement contactRoleSection = $x("//c-cl-contact-role-selector/li");
    public final LightningCombobox contactRolePickList = new LightningCombobox("Select Role");
    public final SelenideElement contactRoleEditButton = contactRoleSection.$(byText("Edit"));
    public final SelenideElement contactRoleApplyButton = contactRoleSection.$(byText("Apply"));
    public final SelenideElement contactRoleInfoText = contactRoleSection.$x(".//*[@class='slds-text-heading_small']");

    //  Lead Qualification section
    public final SelenideElement leadQualificationSection = $x("//c-cl-conversion-info/li");
    public final SelenideElement selectedQualification = leadQualificationSection.$x(".//*[p='Selected Qualification']");

    //  Modal Window (pop-up)
    private final SelenideElement modalWindow = $x("//c-cl-modal//section/div");
    public final SelenideElement modalWindowOkButton = modalWindow.$(byText("Ok"));

    /**
     * Constructor for Lead Convert page.
     * Defines a default Lead Convert page's location in the Lightning Experience.
     */
    public LeadConvertPage() {
        super("Convert Lead");
    }

    /**
     * Constructor for Lead Convert page with iframe's title.
     * Defines Lead Convert page's location using the title of the iframe.
     */
    public LeadConvertPage(String iframeTitle) {
        super(iframeTitle);
    }

    /**
     * Get all the Account items from 'Matched Accounts' table.
     *
     * @return list of account items that contains Account name, RC User ID, RC Account Numbers, etc...
     */
    public List<MatchedItem> getMatchedAccountsList() {
        return matchedAccountsTableItems.asDynamicIterable().stream()
                .map(MatchedItem::new)
                .collect(toList());
    }

    /**
     * Get all the Opportunity items from 'Matched Opportunity' table.
     *
     * @return list of matched opportunity items that contains their Name, Stage, Owner, etc...
     */
    public List<MatchedItem> getMatchedOpportunitiesList() {
        return matchedOpportunitiesTableItems.asDynamicIterable().stream()
                .map(MatchedItem::new)
                .collect(toList());
    }

    /**
     * Get all the Contact items from 'Matched Contacts' table.
     *
     * @return list of matched contact items that contains their Name, Company, Title, etc...
     */
    public List<MatchedItem> getMatchedContactsList() {
        return matchedContactsTableItems.asDynamicIterable().stream()
                .map(MatchedItem::new)
                .collect(toList());
    }

    /**
     * Get a matched item in the 'Matched Accounts' table.
     *
     * @param accountId ID of the matched account to select
     * @return matched account item that contains account's name, RC User ID, RC Account Number, etc...
     */
    public MatchedItem getMatchedAccount(String accountId) {
        var matchedAccountElement = matchedAccountsTableRadioButtons
                .findBy(value(accountId))
                .ancestor("tr");
        return new MatchedItem(matchedAccountElement);
    }

    /**
     * Get a matched item in the 'Matched Opportunities' table.
     *
     * @param opportunityId ID of the matched opportunity to select
     * @return matched opportunity item that contains opportunity's name, stage, owner, etc...
     */
    public MatchedItem getMatchedOpportunity(String opportunityId) {
        var matchedOpportunityElement = matchedOpportunitiesTableRadioButtons
                .findBy(value(opportunityId))
                .ancestor("tr");
        return new MatchedItem(matchedOpportunityElement);
    }

    /**
     * Get a matched item in the 'Matched Contacts' table.
     *
     * @param contactId ID of the matched contact to select
     * @return matched contact item that contains contact's name, company, title, etc...
     */
    public MatchedItem getMatchedContact(String contactId) {
        var matchedContactElement = matchedContactsTableRadioButtons
                .findBy(value(contactId))
                .ancestor("tr");
        return new MatchedItem(matchedContactElement);
    }

    /**
     * Open Lead Convert Page via direct link using Id of the Lead object
     * which is to be converted.
     * <br/>
     * Note: additionally, waits for account lookup input to be visible!
     * For some brands (e.g. Avaya Cloud Office) there is none, so use
     * {@link #openDirect(String)} instead.
     *
     * @param leadId Id of the Lead record that is to be converted
     * @return opened Lead Convert Page reference
     */
    public LeadConvertPage openPage(String leadId) {
        openDirect(leadId);

        newExistingAccountToggle.shouldBe(visible, ofSeconds(60));
        existingAccountSearchInput.getSelf().shouldBe(visible);
        return this;
    }

    /**
     * Open Lead Convert Page via direct link using Id of the Lead object
     * which is to be converted.
     * <br/>
     * Note: useful for some partner leads (e.g. Avaya Cloud Office brand)
     *
     * @param leadId Id of the Lead record that is to be converted
     * @return opened Lead Convert Page reference
     */
    public LeadConvertPage openDirect(String leadId) {
        open(LEAD_CONVERT_PAGE_URL + "?id=" + leadId);
        return this;
    }

    /**
     * Wait until 'Opportunity'/'Opportunity info' section is loaded completely.
     */
    public void waitUntilOpportunitySectionIsLoaded() {
        opportunityInfoSection.shouldBe(visible, ofSeconds(60));
        sleep(2_000);   //  instead of waiting for a "shimmer" that might not appear at all
        opportunityLoadingBar.shouldBe(hidden, ofSeconds(30));
    }

    /**
     * Select an account in 'Matched Accounts' table (select its radio-button and press 'Apply').
     *
     * @param accountId ID of the account to be selected
     */
    public void selectMatchedAccount(String accountId) {
        getMatchedAccount(accountId)
                .getSelectButton()
                .click();
        accountInfoApplyButton.click();
    }

    /**
     * Select an account in 'Matched Accounts' table, when another account is already selected.
     * This method reselects the account by clicking on the corresponding radio button for the account
     * and applies the new choice.
     * <br/>
     * Note: pop-ups handling is included when user clicks 'Edit' in Account Info section.
     *
     * @param newAccountId ID for the new account to be selected
     */
    public void reselectMatchedAccount(String newAccountId) {
        accountInfoEditButton.click();
        modalWindowOkButton.click();
        selectMatchedAccount(newAccountId);
    }

    /**
     * Select a contact in 'Matched Contacts' table (select its radio-button and press 'Apply').
     *
     * @param contactId ID of the contact to be selected
     */
    public void selectMatchedContact(String contactId) {
        getMatchedContact(contactId)
                .getSelectButton()
                .click();
        contactInfoApplyButton.click();
    }

    /**
     * Select a default role for the opportunity's contact in "Opportunity Role" section of the page
     * and confirm the selection (apply).
     */
    public void selectDefaultOpportunityRole() {
        selectOpportunityRole(INFLUENCER_OPPORTUNITY_CONTACT_ROLE);
    }

    /**
     * Select a role for the opportunity's contact in "Opportunity Role" section of the page
     * and confirm the selection (apply).
     *
     * @param roleName value for a chosen role name (e.g. "Influencer", "Decision Maker"...)
     */
    public void selectOpportunityRole(String roleName) {
        contactRolePickList.selectOption(roleName);
        contactRoleApplyButton.click();
    }

    /**
     * Select 'Service' in the 'Opportunity Info' section.
     * <br/>
     * Note: this method is better than direct selection via {@link #servicePickList}
     * because the picklist is too dynamic and slow to load itself and its options,
     * so this method waits long enough to be able to select a given option.
     *
     * @param serviceName any service name for the Opportunity
     *                    (e.g. "Office", "Fax", "Engage Digital Standalone", etc.)
     */
    public void selectService(String serviceName) {
        servicePickList.shouldBe(visible, ofSeconds(20));
        servicePickList.getOptions().shouldHave(itemWithText(serviceName), ofSeconds(20));
        servicePickList.selectOption(serviceName);
    }
}
