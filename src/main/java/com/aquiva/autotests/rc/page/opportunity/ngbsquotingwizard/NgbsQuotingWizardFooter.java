package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard;

import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartPage;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.producttab.ProductsPage;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

/**
 * A Footer section on {@link CartPage}, {@link ProductsPage} and {@link QuotePage}.
 * Shows information about the contract, terms, total amounts in the context of terms.
 */
public class NgbsQuotingWizardFooter {

    public static final String CONTRACT_NONE = "None";
    public static final String CONTRACT_ACTIVE = "Active";

    //  Totals' labels
    public static final String NEW_MONTHLY_RECURRING_CHARGES = "New Monthly Recurring Charges:";
    public static final String NEW_ANNUAL_RECURRING_CHARGES = "New Annual Recurring Charges:";
    public static final String CURRENT_MONTHLY_RECURRING_CHARGES = "Current Monthly Recurring Charges:";
    public static final String CURRENT_ANNUAL_RECURRING_CHARGES = "Current Annual Recurring Charges:";
    public static final String NEW_MONTHLY_DISCOUNT = "New Monthly Discount:";
    public static final String NEW_ANNUAL_DISCOUNT = "New Annual Discount:";
    public static final String CURRENT_MONTHLY_DISCOUNT = "Current Monthly Discount:";
    public static final String CURRENT_ANNUAL_DISCOUNT = "Current Annual Discount:";

    public final SelenideElement footerContainer = $(".billing-details-footer");

    public final SelenideElement billingDetailsAndTermsButton = footerContainer.$("[data-ui-auto='edit-billing-details']");
    public final SelenideElement paymentPlan = footerContainer.$("#payment-plan");
    public final SelenideElement freeServiceCredit = footerContainer.$("#free-service-credit");
    public final SelenideElement freeServiceCreditAmount = footerContainer.$("#fsc-amount");
    public final SelenideElement specialShippingTerms = footerContainer.$("#special-shipping-terms");
    public final SelenideElement contract = footerContainer.$("#contract");
    public final SelenideElement initialTerm = footerContainer.$("#initial-term");
    public final SelenideElement renewalTerm = footerContainer.$("#renewal-term");

    //  Totals
    public final SelenideElement newRecurringChargesLabel = footerContainer.$("#new-recurring-charges .total-label");
    public final SelenideElement newRecurringCharges = footerContainer.$("#new-recurring-charges .total-currency");
    public final SelenideElement currentRecurringChargesLabel = footerContainer.$("#current-recurring-charges .total-label");
    public final SelenideElement currentRecurringCharges = footerContainer.$("#current-recurring-charges .total-currency");
    public final SelenideElement changeInRecurringCharges = footerContainer.$("#recurring-changes .total-currency");
    public final SelenideElement newDiscountLabel = footerContainer.$("#new-discount .total-label");
    public final SelenideElement newDiscount = footerContainer.$("#new-discount .total-currency");
    public final SelenideElement currentDiscountLabel = footerContainer.$("#current-discount .total-label");
    public final SelenideElement currentDiscount = footerContainer.$("#current-discount .total-currency");
    public final SelenideElement changeInDiscount = footerContainer.$("#discount-changes .total-currency");
    public final SelenideElement costOfOneTimeItems = footerContainer.$("#one-time-totals .total-currency-cpq");
}
