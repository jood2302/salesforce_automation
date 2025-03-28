package com.aquiva.autotests.rc.page.salesforce.approval;

import com.aquiva.autotests.rc.page.salesforce.VisualforcePage;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import java.util.Map;

import static com.aquiva.autotests.rc.utilities.Constants.BASE_URL;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.*;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

/**
 * Custom VF page located on the {@link TaxExemptApprovalPage}.
 * Displays information related to Tax Exempt Approvals,
 * e.g. GST Number, SEZ Certificate...
 */
public class TaxExemptionManagerPage extends VisualforcePage {

    //  For the header of Tax Types columns
    public static final String FEDERAL_TAX_TYPE = "Federal tax";
    public static final String STATE_TAX_TYPE = "State tax";
    public static final String COUNTY_TAX_TYPE = "County tax";
    public static final String LOCAL_TAX_TYPE = "Local tax";
    public static final String SALES_TAX_TYPE = "Sales tax";

    //  For the rows of the Tax Exemption statuses
    public static final String REQUESTED_EXEMPTION_STATUS = "Requested";
    public static final String APPROVED_EXEMPTION_STATUS = "Approved";
    public static final String CURRENT_EXEMPTION_STATUS = "Current";
    public static final String REJECTED_EXEMPTION_STATUS = "Rejected";

    public final SelenideElement header = $x("//*[@class='slds-page-header']//h1");
    public final SelenideElement shimmer = $x("//c-placeholder-loading");
    public final SelenideElement spinner = $x("//c-lwc-spinner//*[@role='status']");
    public final SelenideElement errorMessage = $x("//*[contains(@class,'slds-theme_error')]/div[1]");

    public final ElementsCollection taxTypesHeaders =
            $$x("//*[./*[text()='Exemption status']]/following-sibling::*");

    public final SelenideElement saveButton = $(byText("Save"));
    public final SelenideElement submitForApprovalButton = $(byText("Submit for approval"));

    public final SelenideElement gstNumberInput = $x("//*[contains(@id,'gstNumber')]");
    public final SelenideElement sezCertificateLink = $x("//*[label[text()='SEZWOP Certificate']]//a");

    /**
     * Constructor with a default web element for the page's iframe.
     */
    public TaxExemptionManagerPage() {
        super($x("//records-record-layout-section[.//span[text()='Tax Exemptions']]//iframe"));
    }

    /**
     * Open the Tax Exemption Manager page for a given Tax Exempt Approval via direct link.
     *
     * @param approvalId ID of the related Tax Exempt Approval record
     * @return opened Tax Exemption Manager page
     */
    public TaxExemptionManagerPage openPage(String approvalId) {
        open(BASE_URL + "/apex/TaxExemption?id=" + approvalId);
        waitUntilLoaded();
        return this;
    }

    /**
     * Wait until the page is fully loaded.
     * User may safely interact with any of the page's elements after this method is finished.
     */
    public void waitUntilLoaded() {
        header.shouldBe(visible, ofSeconds(30));
        shimmer.shouldBe(hidden, ofSeconds(10));
    }

    /**
     * Return a web element for a checkbox with provided tax type and tax exemption status.
     *
     * @param taxType            any available tax types (e.g. "Federal tax", "Local tax")
     * @param taxExemptionStatus any available exemption status (e.g. "Current", "Requested")
     * @return checkbox that's matched to provided tax type and tax exemption status
     */
    public SelenideElement getExemptionCheckbox(String taxType, String taxExemptionStatus) {
        var taxTypeToIndexMap = Map.of(
                FEDERAL_TAX_TYPE, 1,
                STATE_TAX_TYPE, 2,
                COUNTY_TAX_TYPE, 3,
                LOCAL_TAX_TYPE, 4,
                SALES_TAX_TYPE, 5
        );

        return $x(format("(//c-te-status-item/*[./*='%s']//div/span)[%s]",
                taxExemptionStatus, taxTypeToIndexMap.get(taxType)));
    }

    /**
     * Select the Tax Exemption Status for the given Tax Type
     * via the corresponding checkbox.
     *
     * @param taxType            any available tax types (e.g. "Federal tax", "Local tax")
     * @param taxExemptionStatus any available exemption status (e.g. "Current", "Requested")
     */
    public void setExemptionStatus(String taxType, String taxExemptionStatus) {
        var checkbox = getExemptionCheckbox(taxType, taxExemptionStatus);

        checkbox.$("label").shouldBe(visible, ofSeconds(10)).click();
        checkbox.$("input").shouldBe(checked);
    }

    /**
     * Press 'Save' button,
     * and wait until the save process is finished.
     */
    public void saveChanges() {
        saveButton.click();

        spinner.shouldBe(visible);
        spinner.shouldBe(hidden, ofSeconds(60));
        errorMessage.shouldBe(hidden);
    }

    /**
     * Press 'Submit for approval' button,
     * and wait until the submission process is finished.
     */
    public void clickSubmitForApproval() {
        submitForApprovalButton.click();

        spinner.shouldBe(visible);
        spinner.shouldBe(hidden, ofSeconds(60));
        errorMessage.shouldBe(hidden);
    }
}
