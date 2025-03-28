package com.aquiva.autotests.rc.page.salesforce.approval;

/**
 * The Standard Salesforce page that displays Tax Exempt Approval Page record information,
 * such as Approval details (Approval Name, Approval's Account, etc...), action buttons,
 * related records, custom Tax Exemption Manager (iframe) and many more.
 */
public class TaxExemptApprovalPage extends ApprovalPage {

    //  Tax Exemption Manager
    public final TaxExemptionManagerPage taxExemptionManagerPage = new TaxExemptionManagerPage();

    /**
     * Switch to Tax Exemption Manager Iframe
     * and wait until it's loaded.
     */
    public void switchToTaxExemptionIframe() {
        taxExemptionManagerPage.switchToIFrame();
        taxExemptionManagerPage.waitUntilLoaded();
    }
}