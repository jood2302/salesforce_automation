package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal;

import com.aquiva.autotests.rc.page.components.ContractSelector;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;

/**
 * Modal window in {@link NGBSQuotingWizardPage}
 * activated by clicking on "Billing Details and Terms" button (on "Add Products", "Price" or "Quote Details" tab).
 * <p>
 * You can change charge term, contract, initial, renewal and special terms using this modal.
 * </p>
 */
public class BillingDetailsAndTermsModal {

    //  Free Service Credit section header text with total FSC value
    public static final String FREE_SERVICE_CREDIT_HEADER_TEXT = "Free Service Credit, %s %s";

    //  Info message
    public static final String NOT_AVAILABLE_FOR_CHANGE_ORDER_TOOLTIP = "Not available for Change Order";

    private final SelenideElement dialogContainer = $("uqt-billing-details-modal > div");

    //  Elements
    public final SelenideElement placeholderLoading = dialogContainer.$("placeholder-loading");
    public final SelenideElement paymentPlanToggle = dialogContainer.$(".payment-plan");
    public final ContractSelector contractSelect = new ContractSelector();
    public final SelenideElement contractSelectInput = contractSelect.lwcCheckboxElement.getInput();
    public final SelenideElement contractCheckbox = contractSelect.lwcCheckboxElement.getCheckbox();
    public final SelenideElement initialTermPicklist = dialogContainer.$("[data-ui-auto='initial-term']");
    public final SelenideElement renewalTermPicklist = dialogContainer.$("[data-ui-auto='renewal-term']");
    public final SelenideElement freeShippingTermsPicklist = dialogContainer.$("[data-ui-auto='free-shipping-terms']");
    public final SelenideElement fullMrsToggle = dialogContainer.$(byText("Full MRS"));

    //  Free Service Credit fields for Single-Product Quote
    public final SelenideElement specialTermsPicklist = dialogContainer.$("[data-ui-auto='special-terms']");

    //  Free Service Credit fields for Multi-Product Quote
    public final SelenideElement fscSectionHeader = dialogContainer.$x(".//h3[contains(text(), 'Free Service Credit')]");
    public final ElementsCollection fscServiceHeaders = dialogContainer.$$(".slds-text-heading_small");
    public final SelenideElement specialTermsMvpPicklist = dialogContainer.$("#special-terms-mvp");
    public final SelenideElement specialTermsCcPicklist = dialogContainer.$("#special-terms-cc");
    public final SelenideElement specialTermsEvPicklist = dialogContainer.$("#special-terms-ev");
    public final SelenideElement specialTermsEdPicklist = dialogContainer.$("#special-terms-ed");
    public final SelenideElement fscAmountMvpInput = dialogContainer.$("#fsc-amount-mvp");
    public final SelenideElement fscAmountCcInput = dialogContainer.$("#fsc-amount-cc");
    public final SelenideElement fscAmountEvInput = dialogContainer.$("#fsc-amount-ev");
    public final SelenideElement fscAmountEdInput = dialogContainer.$("#fsc-amount-ed");

    public final SelenideElement cancelButton = dialogContainer.$(byText("Cancel"));
    public final SelenideElement applyButton = dialogContainer.$(byText("Apply"));

    /**
     * Return a charge term option for selection.
     * <p>
     * Useful in case of selecting and checking the state (checked/unchecked).
     *
     * @param chargeTerm name of the Charge Term (e.g. <b>"Monthly", "Annual"</b>)
     */
    public SelenideElement getChargeTermInput(String chargeTerm) {
        return dialogContainer.$("[controlname='chargeTerm'] input#" + chargeTerm.toLowerCase());
    }

    /**
     * Select the charge term with a given name.
     *
     * @param chargeTerm expected option to be selected (e.g. <b>"Monthly", "Annual"</b>)
     */
    public void selectChargeTerm(String chargeTerm) {
        getChargeTermInput(chargeTerm).sibling(0).click();
    }

    /**
     * Set 'Contract' checkbox to be selected or not.
     * <br/>
     * This method has to be used instead of {@link SelenideElement#setSelected(boolean)}
     * because {@code input} element for the 'Contract' is rendered invisible using CSS,
     * and {@link SelenideElement#setSelected(boolean)} only works with visible elements.
     *
     * @param isContractToBeSelected true, if the checkbox should be selected,
     *                               otherwise, it should be deselected.
     */
    public void setContractSelected(boolean isContractToBeSelected) {
        contractSelect.setSelected(isContractToBeSelected);
    }
}
