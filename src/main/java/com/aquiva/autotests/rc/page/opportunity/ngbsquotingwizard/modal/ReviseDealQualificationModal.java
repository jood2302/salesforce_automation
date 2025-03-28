package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal;

import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartPage;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;

/**
 * 'Revise Deal Qualification?' modal window in {@link NGBSQuotingWizardPage}
 * activated by clicking 'Submit for Approval' button on the {@link CartPage}
 * while having an active Deal Qualification.
 */
public class ReviseDealQualificationModal {
    private final SelenideElement dialogContainer = $("submit-for-approval-modal");

    //  Buttons
    public final SelenideElement reviseButton = dialogContainer.$(byText("Revise"));
    public final SelenideElement reviewButton = dialogContainer.$(byText("Review"));
    public final SelenideElement submitImmediatelyButton = dialogContainer.$(byText("Submit Immediately"));
}
