package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal;

import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartPage;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

/**
 * 'View Approvers' modal window in {@link NGBSQuotingWizardPage}
 * shown after clicking DQ Approver button on the {@link CartPage}
 * when the Quote is required for the approval from someone.
 */
public class ViewDqApproversModal {

    //  Approver Levels
    public static final String FINANCE_APPROVAL_LEVEL = "Finance";

    //  Approval Reasons
    public static final String AUTO_RENEW_REMOVAL_REQUESTED_BY_DQ = "Auto-renew removal requested by deal qualification";

    //  Approver Types
    public static final String FINANCE_REVENUE_APPROVER_TYPE = "Finance - Revenue";
    public static final String FINANCE_FPA_APPROVER_TYPE = "Finance - FP&A";

    //  Approver Names
    public static final String FINANCE_REVENUE_APPROVER_NAME = "Finance - Revenue";
    public static final String FINANCE_FPA_APPROVER_NAME = "Finance - FP&A";

    private final SelenideElement dialogContainer = $("dq-approver-modal section > div");

    public final ElementsCollection approverLevels = dialogContainer.$$x(".//dq-approver-table//td[1]");
    public final ElementsCollection approvalReasons = dialogContainer.$$x(".//dq-approver-table//td[2]");
    public final ElementsCollection approverTypes = dialogContainer.$$x(".//dq-approver-table//td[3]");
    public final ElementsCollection approverNames = dialogContainer.$$x(".//dq-approver-table//td[4]");

    public final SelenideElement closeButton = dialogContainer.$(".slds-modal__close");
}
