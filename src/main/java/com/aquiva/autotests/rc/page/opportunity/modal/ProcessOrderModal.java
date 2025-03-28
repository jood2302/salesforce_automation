package com.aquiva.autotests.rc.page.opportunity.modal;

import com.aquiva.autotests.rc.page.components.LightningCombobox;
import com.aquiva.autotests.rc.page.components.SearchableDropdownSelect;
import com.aquiva.autotests.rc.page.opportunity.OpportunityRecordPage;
import com.aquiva.autotests.rc.page.salesforce.GenericSalesforceModal;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThanOrEqual;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.sleep;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

/**
 * Modal window on {@link OpportunityRecordPage}
 * activated by clicking on 'Process Order' button.
 * <br/>
 * This modal window allows user to:
 * <p> - start a Sign-Up process for Opportunity </p>
 * <p> - sync the data with NGBS </p>
 */
public class ProcessOrderModal extends GenericSalesforceModal {

    //  'Sign Up' steps
    public static final String SIGN_UP_MVP_STEP = "Sign Up MVP";

    //  'Sync with NGBS' steps
    public static final String DISCOUNT_SYNC_STEP = "Discount sync";
    public static final String CONTRACT_SYNC_STEP = "Contract sync";
    public static final String CONTRACT_CANCEL_STEP = "Contract cancellation";
    public static final String REPRICE_STEP = "Reprice";
    public static final String UPGRADE_STEP = "Upgrade (in external system)";
    public static final String UP_SELL_STEP = "Up-sell (in external system)";
    public static final String MANUAL_DOWN_SELL_STEP = "Down-sell (in external system)";
    public static final String AUTO_AND_MANUAL_DOWN_SELL_STEP = "Down-sell";
    public static final String ORDER_SYNC = "Order sync";

    //  'Sign Up' ProServ steps
    public static final String CHECK_FOR_MAIN_COST_CENTER_STEP = "Check for Main Cost Center";
    public static final String CREATION_OF_A_MAIN_COST_CENTER_STEP = "Creation of a Main Cost Center";
    public static final String SIGN_UP_PRO_SERV_STEP = "Sign Up ProServ";

    //  'Sign Up' status
    public static final String DATA_VALIDATION_STEP = "Data validation...";
    public static final String READY_TO_REQUEST_FUNNEL_STEP = "Ready to request Funnel";
    public static final String SYNCED_STEP = "Synced";
    public static final String YOUR_ACCOUNT_IS_BEING_PROCESSED_MESSAGE =
            "Your account is being processed. This window can be closed now. " +
                    "You will receive notification in the chatter after completion or use \"Check %s Status\" button";

    //  Tier status
    public static final String SYNCED_WITH_NGBS_STATUS = "Synced with NGBS";
    public static final String NOT_SYNCED_STATUS = "Not Synced";
    public static final String SIGNED_UP_STATUS = "Signed Up";

    //  Platform location select options
    public static final String US1_PLATFORM_LOCATION = "US1";

    //  Language select options
    public static final String EN_US_LANGUAGE = "en-US";

    //  Time zone select options
    public static final String ALASKA_TIME_ZONE = "Alaska";
    public static final String ANCHORAGE_TIME_ZONE = "Anchorage";

    //  Services/Tiers
    public static final String MVP_SERVICE = "MVP";
    public static final String MEETINGS_SERVICE = "Meetings";
    public static final String ENGAGE_VOICE_SERVICE = "Engage Voice";
    public static final String ENGAGE_DIGITAL_SERVICE = "Engage Digital";
    public static final String CONTACT_CENTER_SERVICE = "Contact Center";
    public static final String PRO_SERV_SERVICE = "Professional Services";

    //  'Sign Up' notification messages
    public static final String ACCOUNT_SHOULD_HAVE_PAYMENT_METHOD_ERROR = "%s: Sign Up is not allowed.\n" +
            "Account should have predefined payment method to proceed with Sign Up.";
    public static final String NEED_APPROVED_KYC_REQUEST_ERROR = "%s: SignUp failed\n" +
            "You need an approved KYC request to Sign Up an opportunity";
    public static final String ACCOUNT_SHOULD_HAVE_APPROVED_INVOICING_REQUEST_ERROR = "%s: Sign Up is not allowed.\n" +
            "Account should have approved Invoicing Request Approval to proceed with Sign Up.";
    public static final String DIRECT_DEBIT_THRESHOLD_LIMIT_EXCEEDED_ERROR = "%s: Sign Up is not allowed.\n" +
            "Direct Debit Threshold Limit exceeded. Please, request Direct Debit Approval";
    public static final String DIRECT_DEBIT_MONTHLY_CREDIT_LIMIT_EXCEEDED_ERROR = "%s: Sign Up is not allowed.\n" +
            "Monthly credit limit or signup purchase Limit exceeded. Please, request the new Direct Debit Approval";
    public static final String INVOICE_PAYMENT_METHOD_IS_REQUIRED_FOR_ENGAGE_SIGNUP_ERROR = "%s: Sign Up is not allowed.\n" +
            "Invoice payment method is required";
    public static final String AREA_CODES_ARE_MISSING_ERROR = "%s: SignUp failed\n" +
            "Area Codes are missing. Please select Area Codes for all Items in the Cart of the Primary Quote " +
            "that are eligible for Area Code assignment";
    public static final String YOU_NEED_TO_HAVE_COMPLETED_ENVELOPE_ERROR = "%s: SignUp failed\n" +
            "You need to have Completed Envelope to Sign Up the Account";
    //  TODO Might be changed if the Known Issue PBC-22741 is resolved 
    public static final String OBTAIN_INVOICE_PAYMENT_APPROVAL_ERROR = "%s: Sign Up is not allowed.\n" +
            "You won't be able to sign up this account, please, obtain Invoice payment approval for %s first.";
    public static final String OBTAIN_INVOICE_ON_BEHALF_PAYMENT_APPROVAL_ERROR = "%s: Sign Up is not allowed.\n" +
            "You won’t be able to proceed with this account, please, obtain Invoice-on-behalf payment approval first.";
    public static final String PROVIDE_VAT_NUMBER_ERROR = "%s: Sign Up is not allowed.\n" +
            "Please, provide VAT number or submit and approve Tax exemption Approval.";
    public static final String LINKED_MASTER_ACCOUNT_SHOULD_BE_SIGNED_UP_ERROR = "%s: Linked Master Account should be signed up first\n" +
            "Please Sign Up this Master Account before proceeding";
    public static final String ACTIVE_AGREEMENT_IS_REQUIRED_TO_SIGN_UP_ERROR = "%s: SignUp failed\n" +
            "Active Agreement is required to Sign Up";
    public static final String YOU_NEED_AN_ACTIVE_AGREEMENT_TO_SIGN_UP_ERROR = "%s: Opportunity can't be Signed Up\n" +
            "You need an Active Agreement to Sign Up that Opportunity";
    public static final String REVIEW_LINKED_SERVICE_ELA_ACCOUNTS_ERROR = "%s: Sorry, can't sign up this Opportunity at the moment.\n" +
            "Please, review linked Service ELA Accounts in Account Viewer for next action.";
    public static final String REQUEST_SENT_SUCCESSFULLY = "Request sent successfully. Complete SignUp in Funnel";
    public static final String SERVICE_SUBMITTED_SUCCESSFULLY = "%s submitted successfully";
    public static final String SIGNUP_IS_NOT_AVAILABLE_FOR_VODAFONE_ERROR =
            "%s: The sign-up is unavailable for the Vodafone Business with RingCentral brand.\n" +
                    "Please use the Quoting Tool for Professional Services only";
    public static final String USER_DOES_NOT_HAVE_PERMISSION_TO_SIGN_UP_ENGAGE_ERROR =
            "Sign Up for Engage is not available\n" +
                    "You do not have permission to Sign Up Engage. Please contact Sales Ops for assistance.";
    public static final String PRIMARY_QUOTE_HAS_ERRORS_ERROR = "%s: SignUp failed\n" +
            "Primary quote has errors. Make sure that all errors on primary quote are resolved";
    public static final String MOVE_ALL_PRODUCT_SETS_TO_THE_SHIPPING_GROUPS_ERROR = "%s: Shipping Assignment Info\n" +
            "To proceed with Process Order please move all product sets to the shipping groups.";
    public static final String YOU_HAVE_AN_UNSHIPPED_PRODUCT_SET_WARNING = "%s: Shipping Assignment Info\n" +
            "You have an unshipped product set.";
    public static final String QUOTE_IS_REQUIRED_TO_SIGNUP_ERROR =
            "At least one quote on the opportunity is required to sign up a customer";
    public static final String PLEASE_ADD_ITEMS_IN_CART_ERROR = "%s: SignUp failed\n" +
            "To sign up, please, add any items in the cart";
    public static final String PENDING_TEA_APPROVALS_WARNING = "%s: Tax Exempt Approval is still in progress\n" +
            "You have pending Tax Exempt Approvals. You will be redirected to Sign Up in the next 5 seconds...";

    //  'Sync with NGBS' notification messages
    public static final String OPPORTUNITY_SHOULD_BE_IN_CLOSED_WON_STAGE_ERROR = "%s: Process Order not available\n" +
            "Opportunity should be in Closed Won stage before using Process Order";
    public static final String YOU_DONT_HAVE_PERMISSION_TO_USE_PROCESS_ORDER_ERROR = "%s: Process Order not available\n" +
            "You don't have permission to use Process Order. Please contact Sales Ops for assistance";
    public static final String PRICE_SUCCESSFULLY_CHANGED_MESSAGE = "%s: Process Order\n" +
            "Price has been successfully changed";
    public static final String LICENSES_SHOULD_BE_ADDED_MANUALLY_IN_SW_ERROR = "%s: Error\n" +
            "Licenses are not synced with Service Web. Licenses should be added manually in Service Web.";
    public static final String LICENSES_SHOULD_BE_REMOVED_MANUALLY_ERROR = "%s: Error\n" +
            "Licenses should be removed manually";
    public static final String SYNC_IS_NOT_REQUIRED_FOR_VODAFONE_ERROR =
            "%s: Sync is not required for Vodafone Business with RingCentral\n" +
                    "Please proceed with manual package change in BAP";

    public static final String NOTHING_TO_SYNC_MESSAGE = "%s: Info\nNothing to sync";

    //  Quote Selector elements
    public final SelenideElement pocQuoteRadioButton = dialogContainer.$x(".//span[./*='POC Quote']");
    public final SelenideElement salesQuoteRadioButton = dialogContainer.$x(".//span[./*='Sales Quote']");
    public final SelenideElement salesQuoteRadioButtonInput = salesQuoteRadioButton.$("input");

    public final SelenideElement mvpTierStatus =
            dialogContainer.$("[data-ui-auto='MVP'] .tier-status div[class*='text']");

    public final SelenideElement proServTierStatus =
            dialogContainer.$("[data-ui-auto='Professional Services'] .tier-status div[class*='text']");

    //  Notification elements
    public final SelenideElement alertNotificationBlock = dialogContainer.$("[data-ui-auto='notification-bar']");
    public final ElementsCollection errorNotifications =
            alertNotificationBlock.$$(".theme_error [class*='body'][data-ui-auto='toggle-notifications']");
    public final ElementsCollection warningNotifications =
            alertNotificationBlock.$$(".theme_warning [class*='body'][data-ui-auto='toggle-notifications']");
    public final ElementsCollection successNotifications =
            alertNotificationBlock.$$(".theme_success [class*='body'][data-ui-auto='toggle-notifications']");
    public final ElementsCollection infoNotifications =
            alertNotificationBlock.$$(".theme_info [class*='body'][data-ui-auto='toggle-notifications']");

    //  Steps
    private final String completedStepSelector = ".slds-progress__item.slds-is-completed";
    private final String allStepSelector = ".slds-progress__item";
    private final String syncStepNameSelector = ".step-label";
    private final String signUpStepNameSelector = ".step-header";

    public final ElementsCollection mvpCompletedSyncStepNames =
            dialogContainer.$$(format("[data-ui-auto='MVP'] %s %s", completedStepSelector, syncStepNameSelector));
    public final ElementsCollection engageDigitalSignUpCompletedStepNames =
            dialogContainer.$$(format("[data-ui-auto='Engage Digital'] %s %s", completedStepSelector, signUpStepNameSelector));
    public final ElementsCollection proServCompletedSignUpStepNames =
            dialogContainer.$$(format("[data-ui-auto='Professional Services'] %s %s", completedStepSelector, signUpStepNameSelector));

    public final ElementsCollection mvpAllSyncStepNames =
            dialogContainer.$$(format("[data-ui-auto='MVP'] %s %s", allStepSelector, syncStepNameSelector));
    public final ElementsCollection meetingsAllSyncStepNames =
            dialogContainer.$$(format("[data-ui-auto='Meetings'] %s %s", allStepSelector, syncStepNameSelector));
    public final ElementsCollection proServAllSignUpStepNames =
            dialogContainer.$$(format("[data-ui-auto='Professional Services'] %s %s", allStepSelector, signUpStepNameSelector));

    //  Buttons
    public final SelenideElement closeButton = dialogContainer.$("[data-ui-auto='close-button']");
    public final SelenideElement createCaseButton = dialogContainer.$x(".//button[@title='Create a Case']");
    public final SelenideElement continueButton = dialogContainer.$("[data-ui-auto='quote-continue-button']");

    //  Spinner
    public final SelenideElement spinner = dialogContainer.$x(".//lightning-spinner");
    public final SelenideElement signUpSpinner = dialogContainer.$(".slds-spinner_container.signup-modal");

    //  ### SIGN UP FLOW ###

    //  UI-Less elements: common
    public final SearchableDropdownSelect selectTimeZonePicklist = new SearchableDropdownSelect("Timezone", dialogContainer);
    public final SelenideElement addressVerificationContinueButton =
            dialogContainer.$x(".//c-sn-address-validation-window//*[@data-ui-auto='continue-button']");

    //  UI-Less sections
    public final SelenideElement mvpSection = dialogContainer.$("[data-ui-auto='MVP']");
    public final SelenideElement engageVoiceSection = dialogContainer.$("[data-ui-auto='Engage Voice']");
    public final SelenideElement engageDigitalSection = dialogContainer.$("[data-ui-auto='Engage Digital']");
    public final SelenideElement contactCenterSection = dialogContainer.$("[data-ui-auto='Contact Center']");
    public final SelenideElement proServSection = dialogContainer.$("[data-ui-auto='Professional Services']");

    //  UI-Less elements: MVP
    /**
     * @see #waitUntilMvpPreparingDataStepIsCompleted
     */
    public final SelenideElement mvpPreparingDataActiveStep =
            dialogContainer.$("[data-ui-auto='mvp-step']");
    public final SelenideElement signUpMvpStatus =
            dialogContainer.$x(".//*[*='Sign Up MVP']//div[contains(@class,'text')]");

    //  UI-Less elements: Meetings
    public final SelenideElement meetingsPreparingDataActiveStep =
            dialogContainer.$("[data-ui-auto='mvp-step']");
    public final SelenideElement signUpMeetingsStatus =
            dialogContainer.$x(".//*[*='Sign Up Meetings']//*[contains(@class,'text')]");

    //  UI-Less elements: Engage
    public final SelenideElement engagePreparingDataActiveStep =
            dialogContainer.$("[data-ui-auto='engage-step']");
    public final LightningCombobox engageVoicePlatformLocationSelect =
            new LightningCombobox(engageVoiceSection.$x(".//*[@data-ui-auto='platform-location']"));
    public final SearchableDropdownSelect engageVoiceTimezoneSelect =
            new SearchableDropdownSelect("Timezone", engageVoiceSection);
    public final LightningCombobox engageDigitalPlatformLocationSelect =
            new LightningCombobox(engageDigitalSection.$x(".//*[@data-ui-auto='platform-location']"));
    public final LightningCombobox engageDigitalLanguageSelect =
            new LightningCombobox(engageDigitalSection.$x(".//*[@data-ui-auto='language']"));
    public final SearchableDropdownSelect engageDigitalTimezoneSelect =
            new SearchableDropdownSelect("Timezone", engageDigitalSection);
    public final SelenideElement engageDigitalDomainInput =
            dialogContainer.$("[name='rcEngageDigitalDomain']");
    public final SelenideElement signUpEngageStatus =
            dialogContainer.$x(".//*[*='Sign Up Engage']//*[contains(@class,'text')]");

    //  UI-Less elements: Engage Voice Address
    public final SelenideElement engageVoiceAddress1Input = engageVoiceSection.$x(".//input[@name='address1']");
    public final SelenideElement engageVoiceAddress2Input = engageVoiceSection.$x(".//input[@name='address2']");
    public final LightningCombobox engageVoiceCountrySelect = new LightningCombobox("Country", engageVoiceSection);
    public final LightningCombobox engageVoiceStateSelect = new LightningCombobox("State", engageVoiceSection);
    public final SelenideElement engageVoiceCityInput = engageVoiceSection.$x(".//*[@data-id='city']//input");
    public final SelenideElement engageVoiceZipInput = engageVoiceSection.$x(".//*[@data-id='zip']//input");
    public final SelenideElement engageVoiceValidateAddressButton = engageVoiceSection.$x(".//*[@data-ui-auto='validate-button']");

    //  UI-Less elements: Engage Digital Address
    public final SelenideElement engageDigitalAddress1Input = engageDigitalSection.$x(".//input[@name='address1']");
    public final SelenideElement engageDigitalAddress2Input = engageDigitalSection.$x(".//input[@name='address2']");
    public final LightningCombobox engageDigitalCountrySelect = new LightningCombobox("Country", engageDigitalSection);
    public final LightningCombobox engageDigitalStateSelect = new LightningCombobox("State", engageDigitalSection);
    public final SelenideElement engageDigitalCityInput = engageDigitalSection.$x(".//*[@data-id='city']//input");
    public final SelenideElement engageDigitalZipInput = engageDigitalSection.$x(".//*[@data-id='zip']//input");
    public final SelenideElement engageDigitalValidateAddressButton = engageDigitalSection.$x(".//*[@data-ui-auto='validate-button']");

    //  UI-Less elements: Contact Center
    public final SelenideElement rcCcPreparingDataActiveStep = dialogContainer.$("[data-ui-auto='contact-center-step']");
    public final SearchableDropdownSelect rcCcTimezoneSelect = new SearchableDropdownSelect("*Timezone", contactCenterSection);
    public final LightningCombobox selectGeoRegionPicklist = new LightningCombobox("Geo Region", dialogContainer);
    public final LightningCombobox selectImplementationTeamPicklist =
            new LightningCombobox("Implementation Team", dialogContainer);
    public final LightningCombobox selectInContactSegmentPicklist =
            new LightningCombobox("InContact Segment", dialogContainer);
    public final SelenideElement ccNumberInput = dialogContainer.$x(".//input[@name='ccNumber']");
    public final ElementsCollection orderToVendorTableRows = dialogContainer.$$x(".//c-sn-order-to-vendor-table//tbody/tr");
    public final SelenideElement contactCenterValidateButton = dialogContainer.$x(".//*[@data-ui-auto='validate-button']");

    //  UI-Less elements: Professional Services
    public final SelenideElement signUpProServStatus =
            dialogContainer.$x(".//*[*='Sign Up ProServ']//div[contains(@class,'text')]");

    //  Buttons
    public final SelenideElement mvpExpandButton = mvpSection.$x(".//*[@data-ui-auto='show-hide-tier']");
    public final SelenideElement engageVoiceExpandButton = engageVoiceSection.$x(".//*[@data-ui-auto='show-hide-tier']");
    public final SelenideElement engageDigitalExpandButton = engageDigitalSection.$x(".//*[@data-ui-auto='show-hide-tier']");
    public final SelenideElement contactCenterExpandButton = contactCenterSection.$x(".//*[@data-ui-auto='show-hide-tier']");
    public final SelenideElement professionalServicesExpandButton = proServSection.$x(".//*[@data-ui-auto='show-hide-tier']");
    public final SelenideElement signUpButton = dialogContainer.$("[data-ui-auto='signup-service-button']");
    public final SelenideElement processEngageVoiceButton = dialogContainer.$(byText("Process Engage Voice"));
    public final SelenideElement processEngageDigitalButton = dialogContainer.$(byText("Process Engage Digital"));
    public final SelenideElement processContactCenterButton = dialogContainer.$(byText("Process Contact Center"));
    public final SelenideElement signUpProfessionalServicesButton = dialogContainer.$(byText("Sign Up Professional Services"));
    public final SelenideElement processProfessionalServicesButton = dialogContainer.$(byText("Process Professional Services"));
    public final SelenideElement checkProfessionalServicesStatusButton = dialogContainer.$(byText("Check Professional Services Status"));

    //  Tabs
    public final SelenideElement signUpTab = dialogContainer.$x(".//*[@title='Sign Up']");
    public final SelenideElement adminPreviewTab = dialogContainer.$x(".//*[@title='Admin Preview']");

    //  Admin Preview tab
    public final SelenideElement signUpBodyContents = dialogContainer.$x(".//pre");

    //  ### SYNC WITH NGBS FLOW ####

    //  Buttons
    public final SelenideElement nextButton = dialogContainer.$("[data-ui-auto='sync-next-button']");

    /**
     * Constructor for the modal window to locate it via its default header.
     */
    public ProcessOrderModal() {
        super("Process Order");
    }

    /**
     * Get the web element for the Sync step by its displayed name.
     *
     * @param stepName name of the step (see 'Sync with NGBS' steps constants,
     *                 e.g. {@link #DISCOUNT_SYNC_STEP}, {@link #CONTRACT_SYNC_STEP}, etc.)
     */
    public SelenideElement getSyncStepElement(String stepName) {
        return dialogContainer.$x(".//li[.//*[@class='step-label']='" + stepName + "']");
    }

    /**
     * Wait until the 'Preparing Data' step for the MVP service
     * during sign-up process is successfully completed without errors.
     * <br/>
     * Note: the 'warnings' still could be displayed in some cases, see {@link #warningNotifications}.
     */
    public void waitUntilMvpPreparingDataStepIsCompleted() {
        mvpPreparingDataActiveStep.shouldHave(exactTextCaseSensitive(READY_TO_REQUEST_FUNNEL_STEP), ofSeconds(120));
        errorNotifications.shouldHave(size(0));
    }

    /**
     * Expand notifications in the notification bar.
     */
    public void expandNotifications() {
        var isCollapsed = alertNotificationBlock
                .shouldBe(visible)
                .has(cssClass("notifications--closed"));
        if (isCollapsed) {
            alertNotificationBlock.click();
        }
    }

    /**
     * Expand the 'Contact Center' section,
     * and wait until the section is fully expanded.
     */
    public void expandContactCenterSection() {
        contactCenterExpandButton.click();
        sleep(2_000);   //  some available elements might not work during the animation of the expanding section
    }

    /**
     * Click the 'Process Contact Center' button in the Contact Center section
     * to initiate sign up process, and check that this process has ended successfully.
     */
    public void clickProcessContactCenter() {
        processContactCenterButton.click();

        signUpSpinner.shouldBe(visible, ofSeconds(10));
        signUpSpinner.shouldBe(hidden, ofSeconds(120));
        errorNotifications.shouldHave(size(0));
        rcCcPreparingDataActiveStep.shouldHave(exactTextCaseSensitive(SYNCED_STEP), ofSeconds(60));
        orderToVendorTableRows.shouldHave(sizeGreaterThanOrEqual(2));
        sleep(2_000);   //  some available elements might not work during the animation of the expanding section 
    }

    /**
     * Select the default value in the 'Timezone' selector
     * when it becomes available for selection
     * (it might be really slow to fetch the timezones from the Funnel API)
     */
    public void selectDefaultTimezone() {
        selectTimeZonePicklist.getInput().shouldBe(enabled, ofSeconds(30));
        selectTimeZonePicklist.selectOptionContainingText(ALASKA_TIME_ZONE);
    }

    /**
     * Select first option in custom combobox (RC Engage Voice/Digital Platform).
     *
     * @param engageService name of the Engage service (e.g. "Engage Voice", "Engage Digital")
     */
    public void selectRcEngagePlatformFirstOption(String engageService) {
        var rcEngagePlatformSelect =
                dialogContainer.$x(".//c-custom-combobox[.//*='RC " + engageService + " Platform']");

        rcEngagePlatformSelect.$x(".//button").click();
        rcEngagePlatformSelect.$x(".//ul/li").click();
    }
}
