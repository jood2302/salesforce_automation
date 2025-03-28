package com.aquiva.autotests.rc.page.salesforce.cases.modal;

import com.aquiva.autotests.rc.page.components.LightningCombobox;
import com.aquiva.autotests.rc.page.components.lookup.StandardLwcLookupComponent;
import com.aquiva.autotests.rc.page.salesforce.RecordCreationModal;
import com.aquiva.autotests.rc.page.salesforce.cases.CaseRecordPage;
import com.codeborne.selenide.SelenideElement;
import com.sforce.soap.enterprise.sobject.Case;

import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.sleep;
import static java.time.Duration.ofSeconds;

/**
 * Modal window in {@link CaseRecordPage} activated by clicking on "Create a Case" button
 * on Opportunity/Lead/Case record page, or Quote Wizard, or Process Order modal window.
 * <p>
 * This dialog creates {@link Case} object for Opportunity, Lead or Case object.
 * </p>
 */
public class CreateCaseModal extends RecordCreationModal {

    //  For 'Case Record Type' field
    public static final String DEAL_AND_ORDER_SUPPORT_CASE_RECORD_TYPE = "Deal and Order Support";

    //  For 'Case Origin' picklist
    public static final String OPPORTUNITY_ORIGIN = "Opportunity";
    public static final String QUOTING_PAGE_ORIGIN = "Quoting Page";
    public static final String PROCESS_ORDER_ORIGIN = "Process Order";
    public static final String LEAD_ORIGIN = "Lead";

    //  For 'Case Category' picklist
    public static final String APPROVAL_REQUEST_CATEGORY = "Approval Request";
    public static final String SYSTEM_ISSUE_CATEGORY = "System Issue";
    public static final String QUOTING_ASSISTANCE_CATEGORY = "Quoting Assistance";
    public static final String PROVISIONING_ASSISTANCE_CATEGORY = "Provisioning Assistance";
    public static final String IGNITE_PARTNER_CASE_CATEGORY = "Ignite Partner";

    //  For 'Case Subcategory' picklist
    public static final String DEAL_REVIEW_REQUEST_SUBCATEGORY = "Deal Review Request";
    public static final String DEMAND_FUNNEL_SUBCATEGORY = "Demand Funnel";
    public static final String CONTACT_CENTER_SUBCATEGORY = "Contact Center";
    public static final String LEAD_SUBCATEGORY = "Lead";

    //  For 'Case Dependencies' picklist
    public static final String NONE_CASE_DEPENDENCIES = "--None--";

    //  For 'Priority' picklist
    public static final String MEDIUM_PRIORITY_VALUE = "Medium";

    //  For 'Status' picklist
    public static final String NEW_STATUS_VALUE = "New";

    /**
     * For Sales Reps, Admins, etc.
     *
     * @see #caseRecordTypeLookupInput
     */
    public final SelenideElement caseRecordTypeOutput = dialogContainer.$(".recordTypeName");
    /**
     * For Deal Desk users.
     *
     * @see #caseRecordTypeOutput
     */
    public final SelenideElement caseRecordTypeLookupInput =
            new StandardLwcLookupComponent(dialogContainer.$x(".//*[./label='Case Record Type']//div[@class='slds-combobox_container']")).getInput();
    public final StandardLwcLookupComponent accountLookup =
            new StandardLwcLookupComponent(dialogContainer.$x(".//*[./label='Account Name']//div[@class='slds-combobox_container']"));
    public final StandardLwcLookupComponent opportunityReferenceLookup =
            new StandardLwcLookupComponent(dialogContainer.$x(".//*[./label='Opportunity Reference']//div[@class='slds-combobox_container']"));
    public final StandardLwcLookupComponent leadLookup =
            new StandardLwcLookupComponent(dialogContainer.$x(".//*[./label='Lead']//div[@class='slds-combobox_container']"));
    public final LightningCombobox caseCategoryPicklist = new LightningCombobox("Case Category", dialogContainer);
    /**
     * For Sales Reps, Admins, etc.
     *
     * @see #caseOriginLookupInput
     */
    public final SelenideElement caseOriginOutput = dialogContainer.$x(".//*[@field-label='Case Origin']//*[@slot='outputField']");
    /**
     * For Deal Desk users.
     *
     * @see #caseOriginOutput
     */
    public final SelenideElement caseOriginLookupInput = new LightningCombobox("Case Origin", dialogContainer).getInput();

    public final StandardLwcLookupComponent csmLookup =
            new StandardLwcLookupComponent(dialogContainer.$x(".//*[./label='CSM']//div[@class='slds-combobox_container']"));
    public final StandardLwcLookupComponent accountOwnerLookup =
            new StandardLwcLookupComponent(dialogContainer.$x("..//*[./label='Account Owner']//div[@class='slds-combobox_container']"));
    public final StandardLwcLookupComponent parentPartnerAccountLookup =
            new StandardLwcLookupComponent(dialogContainer.$x("..//*[./label='Parent Partner Account']//div[@class='slds-combobox_container']"));

    public final SelenideElement description = dialogContainer.$x(".//label[text()='Description']/following-sibling::div/textarea");
    public final LightningCombobox caseSubcategoryPicklist = new LightningCombobox("Case Subcategory", dialogContainer);
    public final LightningCombobox caseDependenciesPicklist = new LightningCombobox("Case Dependencies", dialogContainer);
    public final SelenideElement subjectInput = dialogContainer.$x(".//*[@name='Subject']");

    //  'Description Information' section ('Internal Business Services' record type)
    public final StandardLwcLookupComponent affectedUserLookup =
            new StandardLwcLookupComponent(dialogContainer.$x(".//*[./label='*Affected User']//div[@class='slds-combobox_container']"));
    public final SelenideElement stepsToReproduceTextArea = dialogContainer.$x(".//*[@name='Steps_to_Reproduce__c']");
    public final SelenideElement expectedResultTextArea = dialogContainer.$x(".//*[@name='Expected_Result__c']");
    public final SelenideElement actualResultTextArea = dialogContainer.$x(".//*[@name='Actual_Result__c']");
    public final SelenideElement linkToAffectedRecordInput = dialogContainer.$x(".//*[@name='Error_Link__c']");

    /**
     * Constructor for {@link Case} record creation modal window
     * with initialization of its dialog container's locator using its header's title.
     */
    public CreateCaseModal() {
        super("New Case");
    }

    /**
     * Wait until the modal is loaded and completely ready to be interacted with.
     */
    public void waitUntilLoaded() {
        accountLookup.getSelf().shouldBe(visible, ofSeconds(20));
        sleep(1_000);   // helps to avoid picklists options display reset in tests
    }
}
