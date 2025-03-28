package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.dealqualificationtab;

import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.sforce.soap.enterprise.sobject.Product2;

import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$x;
import static java.time.Duration.ofSeconds;

/**
 * 'Deal Qualification' tab in {@link NGBSQuotingWizardPage}.
 * <br/>
 * Used for checking and approving deal qualifications.
 */
public class DealQualificationPage extends NGBSQuotingWizardPage {

    //  Approval Statuses
    public static final String APPROVAL_STATUS_REVISION_PENDING = "Approval Status: Revision Pending";
    public static final String APPROVAL_STATUS_PENDING_APPROVAL = "Approval Status: Pending Approval";
    public static final String PENDING_APPROVAL_STATUS = "Pending";

    //  Approver Reasons
    public static final String AUTO_RENEW_REMOVAL_REQUESTED_BY_DQ = "Auto-renew removal requested by deal qualification";

    //  Elements
    public final SelenideElement loadingMessage = $("dq-details").$(byText("loading..."));
    public final SelenideElement dqApprovalStatus = $(".dq-details-badge");

    //  'License Requirements' section
    public final SelenideElement licenseRequirementsSection = $("dq-discounts-multiproduct");
    public final ElementsCollection licenseRequirementsPackages = licenseRequirementsSection.$$("#package");
    public final ElementsCollection licenseRequirementsLicenses = licenseRequirementsSection.$$("#license");
    public final ElementsCollection licenseRequirementsQuantities = licenseRequirementsSection.$$("#quantity");
    public final ElementsCollection licenseRequirementsDiscounts = licenseRequirementsSection.$$("#discount");

    //  'Finance' subsection of 'Approvals' section
    public final ElementsCollection financeApprovalStatuses =
            $$x("//th[text()='Finance']/following-sibling::tr[following-sibling::th]/td[3]");
    public final ElementsCollection financeApproverReasons =
            $$x("//th[text()='Finance']/following-sibling::tr[following-sibling::th]/td[8]");

    //  Buttons
    public final SelenideElement reviseButton = $("[data-ui-auto='dq-details-revise-button']");

    /**
     * @see #submitForApproval()
     */
    public final SelenideElement submitForApprovalButton = $("[data-ui-auto='dq-details-submit-for-approval-button']");

    /**
     * Get one of the 'License Requirements' items on the Deal Qualification tab.
     *
     * @param licenseName license name selected in the 'License' column of the item.
     *                    Make sure to use Product's name as it's set on the corresponding {@link Product2#getName()}.
     *                    <br/>
     *                    (e.g. 'DigitalLine Unlimited' instead  of 'DigitalLine Unlimited Core' as on the Add Products/Price tabs)
     * @return composite object to extract other parameters from (service, quantity, discount...)
     */
    public LicenseRequirementsItem getLicenseRequirementsItem(String licenseName) {
        return new LicenseRequirementsItem(licenseName);
    }

    /**
     * {@inheritDoc}
     */
    public void waitUntilLoaded() {
        super.waitUntilLoaded();
        loadingMessage.shouldBe(hidden, ofSeconds(60));
        dqApprovalStatus.shouldBe(visible, ofSeconds(60));
    }

    /**
     * Open the Deal Qualification tab by clicking on the tab's button.
     */
    public DealQualificationPage openTab() {
        dealQualificationTabButton.click();
        waitUntilLoaded();
        return this;
    }

    /**
     * Click 'Submit for Approval' button on the Deal Qualification tab.
     */
    public void submitForApproval() {
        submitForApprovalButton.click();

        progressBar.shouldBe(visible);
        waitUntilLoaded();
    }
}
