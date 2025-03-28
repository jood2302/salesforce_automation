package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.psphasestab;

import com.codeborne.selenide.SelenideElement;

/**
 * A single Phase Line item on the existing {@link PhaseItem} of the {@link PsPhasesPage}.
 * <br/>
 * It contains the fields like Product Name, Payment Plan, Item and Phase Price, Quantities, etc.
 * <br/>
 * Normally, to finalize the quote creation, the user should assign all the ProServ products (as Cart Items)
 * to the one or more Phases on the {@link PsPhasesPage}.
 */
public class PhaseLineItem {
    //  For 'Total Qty' field
    public static final String TOTAL_QTY_FORMAT = "/ %d";

    private final SelenideElement phaseLineItemElement;

    /**
     * Constructor for the Phase Line Item that defines its position on the web page.
     *
     * @param phaseLineItemElement web element for the main container of the item
     */
    public PhaseLineItem(SelenideElement phaseLineItemElement) {
        this.phaseLineItemElement = phaseLineItemElement;
    }

    /**
     * Get the main container of the Phase Line Item.
     */
    public SelenideElement getSelf() {
        return phaseLineItemElement;
    }

    /**
     * Get the Product Name of the Phase Line Item
     * (e.g. 'Additional Data Collection Session').
     */
    public SelenideElement getProductName() {
        return phaseLineItemElement.$("#product-name");
    }

    /**
     * Get the Payment Plan of the Phase Line Item
     * (i.e. charge term, "One - Time", "Monthly", etc.).
     */
    public SelenideElement getPaymentPlan() {
        return phaseLineItemElement.$("#payment-plan");
    }

    /**
     * Get the combined element for the both single Item and total Item prices.
     * Includes the quote's currency (e.g. "USD 1000.00 / 10000.00", for qty = 10).
     */
    public SelenideElement getItemAndPhasePrice() {
        return phaseLineItemElement.$("#item-and-phase-price");
    }

    /**
     * Get the input element of the Quantity field for the Phase Line Item.
     */
    public SelenideElement getQuantityInput() {
        return phaseLineItemElement.$("#quantity");
    }

    /**
     * Get the 'Total Quantity' field for the Phase Line Item.
     * <br/>
     * Represents the max quantity of the ProServ product that user added on the Price tab.
     *
     * @see #TOTAL_QTY_FORMAT
     */
    public SelenideElement getTotalQuantity() {
        return phaseLineItemElement.$("#total-quantity");
    }

    /**
     * Get the button to remove the Phase Line Item from the Phase.
     */
    public SelenideElement getRemoveButton() {
        return phaseLineItemElement.$("#remove-product-button");
    }
}
