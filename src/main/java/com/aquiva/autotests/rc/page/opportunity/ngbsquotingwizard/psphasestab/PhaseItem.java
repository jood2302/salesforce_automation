package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.psphasestab;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

/**
 * A single block on the {@link PsPhasesPage} from the "Phases" section.
 */
public class PhaseItem {
    private final SelenideElement phaseContainer;

    /**
     * Constructor for the Phase Item that defines its position on the web page.
     *
     * @param phaseContainer web element for the main container of the item
     */
    public PhaseItem(SelenideElement phaseContainer) {
        this.phaseContainer = phaseContainer;
    }

    /**
     * Get actual web element behind Phase Item component.
     */
    public SelenideElement getSelf() {
        return phaseContainer;
    }

    /**
     * Get the 'Add Product' button for the Phase.
     * Allows to assign ProServ Cart Items to the current phase.
     */
    public SelenideElement getAddProductButton() {
        return phaseContainer.$("#add-product");
    }

    /**
     * Get all Phase Line Items that are present on the Phase
     * as web elements.
     */
    public ElementsCollection getAllPhaseLineItems() {
        return phaseContainer.$$("tbody tr");
    }

    /**
     * Get Phase Line Item that belongs to the Phase.
     *
     * @param productName displayed name of the Phase Line Item (as a product name)
     */
    public PhaseLineItem getPhaseLineItem(String productName) {
        return new PhaseLineItem(phaseContainer.$x(".//tr[.//*[@id='product-name'][text()='" + productName + "']]"));
    }

    /**
     * Get the 'Move all unassigned items here' button for the Phase.
     * <br/>
     * It assigns all the remaining products from the 'Cart Items' section
     * to the current Phase.
     */
    public SelenideElement getMoveAllUnassignedItemsHereButton() {
        return phaseContainer.$("#move-all-unassigned-items-here");
    }

    /**
     * Get the 'delete' button for the Phase.
     */
    public SelenideElement getDeleteButton() {
        return phaseContainer.$("#delete-phase");
    }
}
