package com.aquiva.autotests.rc.page.opportunity;

import com.aquiva.autotests.rc.page.salesforce.RecordPage;
import com.codeborne.selenide.SelenideElement;

import static com.aquiva.autotests.rc.utilities.Constants.BASE_URL;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;
import static java.time.Duration.ofSeconds;

/**
 * Opportunity Contact Role page that shows Contact Role for the account and with primary and secondary contact roles.
 */
public class OpportunityContactRolePage extends RecordPage {

    public static final String CONTACT_ROLE_CANNOT_BE_MODIFIED_ERROR =
            "Contact Role cannot be modified. Please contact Deal Desk for any questions.";
    public static final String OPPORTUNITY_CONTACT_ROLE_WAS_SAVED = "Opportunity Contact Role was saved.";

    public final SelenideElement editButton = $("a[title='Edit']");
    public final SelenideElement primaryRoleCheckBox = $("[data-aura-class*='uiInputCheckbox']");
    public final SelenideElement saveButton = $("button[title='Save']");
    public final SelenideElement toastSuccessMessage = $("[data-aura-class='forceActionsText']");
    public final SelenideElement errorBlock = $(".errorsList");

    /**
     * Open the Opportunity Contact Role page for the given object
     *
     * @param opportunityContactRoleId opportunity contact role object id
     * @return Opened Opportunity Contact Role page
     */
    public OpportunityContactRolePage openPage(String opportunityContactRoleId) {
        open(BASE_URL + "/" + opportunityContactRoleId);
        waitUntilLoaded();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void waitUntilLoaded() {
        editButton.shouldBe(visible, ofSeconds(30));
        entityTitle.shouldBe(visible, ofSeconds(10));
    }
}
