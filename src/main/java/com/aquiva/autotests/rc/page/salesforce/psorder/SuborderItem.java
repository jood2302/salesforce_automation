package com.aquiva.autotests.rc.page.salesforce.psorder;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.sforce.soap.enterprise.sobject.Suborder__c;

import java.util.List;

import static com.codeborne.selenide.Selenide.$x;

/**
 * Page Object for the Suborder item in Salesforce
 * that's located inside {@link ProServSuborderPage}.
 */
public class SuborderItem {
    private final SelenideElement suborderElement;

    /**
     * Constructor for the Suborder item that defines its position on the web page.
     *
     * @param suborderElement web element for the main container of the item
     */
    public SuborderItem(SelenideElement suborderElement) {
        this.suborderElement = suborderElement;
    }

    /**
     * Constructor for the Suborder item
     * that defines its position on the web page by the Suborder name.
     *
     * @param suborderName name of the Suborder item, see {@link Suborder__c#getName()}
     */
    public SuborderItem(String suborderName) {
        this.suborderElement =
                $x("//pro-serv-phase[.//h1[@id='phase-name-number'][text()=' " + suborderName + " ']]");
    }

    /**
     * Get a web element for an expand/collapse button of the Suborder item.
     */
    public SelenideElement getExpandCollapseButton() {
        return suborderElement.$("[data-ui-auto='expand-collapse-phase']");
    }

    /**
     * Get a web element for a 'Revenue Category' field of the Suborder item.
     */
    public SelenideElement getRevenueCategory() {
        return suborderElement.$("#revenue-category");
    }

    /**
     * Get a web element for a 'Add Product' button of the Suborder item.
     */
    public SelenideElement getAddProductButton() {
        return suborderElement.$("#add-product");
    }

    /**
     * Get a web element for a 'Move All Assigned Items here' button of the Suborder item.
     */
    public SelenideElement getMoveAllAssignedItemsHereButton() {
        return suborderElement.$("#move-all-unassigned-items-here");
    }

    /**
     * Get a web element for a 'Process Suborder' button of the Suborder item.
     */
    public SelenideElement getProcessSuborderButton() {
        return suborderElement.$("#process-suborder");
    }

    /**
     * Get a web element for a button of the deleting of the Suborder item.
     */
    public SelenideElement getDeleteSuborderButton() {
        return suborderElement.$("#delete-phase");
    }

    /**
     * Get a list of all Suborder Product Line Items from the Suborder item.
     *
     * @return list of {@link SuborderProductLineItem} objects within the Suborder item
     */
    public List<SuborderProductLineItem> getAllSuborderProductLineItems() {
        return suborderElement.$$x(".//tr[.//*[@id='product-name']]").stream()
                .map(SuborderProductLineItem::new)
                .toList();
    }

    /**
     * Get list of the names of all Suborder Licenses from the Suborder item.
     */
    public ElementsCollection getAllSuborderLicenseNames() {
        return suborderElement.$$("#product-name");
    }
}
