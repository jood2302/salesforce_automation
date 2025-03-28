package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.psphasestab;

import com.codeborne.selenide.SelenideElement;

/**
 * A single Cart Item in the 'Cart Item' section on the {@link PsPhasesPage}.
 * <br/>
 * Represents one of the added ProServ products that should be assigned to the one or more Phases.
 */
public class PsCartItem {
    //  For 'Qty' field
    public static final String QTY_FORMAT = "%d / %d";

    private final SelenideElement psCartItemElement;

    /**
     * Constructor for the ProServ Cart Item that defines its position on the web page.
     *
     * @param psCartItemElement web element for the main container of the item
     */
    public PsCartItem(SelenideElement psCartItemElement) {
        this.psCartItemElement = psCartItemElement;
    }

    /**
     * Get the main web element of the ProServ Cart Item.
     */
    public SelenideElement getSelf() {
        return psCartItemElement;
    }

    /**
     * Get the 'Qty' value of the ProServ Cart Item.
     * <br/>
     * Contains both unassigned (available) and max quantity of the ProServ product
     * in the following format "Unassigned / Total" (e.g. "7 / 10").
     *
     * @see #QTY_FORMAT
     */
    public SelenideElement getQuantity() {
        return psCartItemElement.$("#unassigned-and-total-quantity");
    }
}
