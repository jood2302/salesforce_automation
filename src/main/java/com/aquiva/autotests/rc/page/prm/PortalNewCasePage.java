package com.aquiva.autotests.rc.page.prm;

import com.aquiva.autotests.rc.page.components.LightningCombobox;
import com.aquiva.autotests.rc.page.components.lookup.CustomLwcLookupComponent;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;
import static java.lang.String.format;

/**
 * Page Object for the PRM New Case page.
 */
public class PortalNewCasePage {
    public final static String CASE_RECORD_WAS_CREATED_MESSAGE = "Case record was created. Current tab will be closed.";

    private static final String INPUT_FIELD_FORMAT = "//*[./text()=\"%s\"]/ancestor::*[1]//input";

    //  Notification block
    private final SelenideElement notificationContainer = $("[class='forceVisualMessageQueue']");
    public final SelenideElement notificationBar = notificationContainer.$("[data-aura-class='forceActionsText']");

    //  Elements
    public final SelenideElement header = $x("//div[text()='New Case: Deal And Order Support']");

    //  Reference Information section
    public final CustomLwcLookupComponent accountNameLookup =
            new CustomLwcLookupComponent($x(".//div[./*='Account Name']"));
    public final CustomLwcLookupComponent opportunityReferenceLookup =
            new CustomLwcLookupComponent($x(".//div[./*='Opportunity Reference']"));
    public final CustomLwcLookupComponent csmLookup =
            new CustomLwcLookupComponent($x(".//div[./*='CSM']"));
    public final CustomLwcLookupComponent caseRecordTypeLookup =
            new CustomLwcLookupComponent($x(".//div[./*='Case Record Type']"));
    public final CustomLwcLookupComponent leadLookup =
            new CustomLwcLookupComponent($x(".//div[./*='Lead']"));
    public final CustomLwcLookupComponent accountOwnerLookup =
            new CustomLwcLookupComponent($x(".//div[./*='Account Owner']"));

    //  Case Information section
    public final LightningCombobox priorityPicklist = new LightningCombobox("Priority");
    public final LightningCombobox statusPicklist = new LightningCombobox("Status");
    public final LightningCombobox caseCategoryPicklist = new LightningCombobox("Case Category");
    public final LightningCombobox caseOriginPicklist = new LightningCombobox("Case Origin");
    public final LightningCombobox caseSubcategoryPicklist = new LightningCombobox("Case Subcategory");

    //  Description Information section
    public final SelenideElement subjectInput = $x(format(INPUT_FIELD_FORMAT, "Subject"));
    public final SelenideElement descriptionInput = $x(format(INPUT_FIELD_FORMAT, "Description"));

    public final SelenideElement saveButton = $x("//button[text()='Save']");
}
