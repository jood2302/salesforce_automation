package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.dealqualificationtab;

import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartPage;
import com.codeborne.selenide.SelenideElement;
import com.sforce.soap.enterprise.sobject.DQ_Deal_Qualification_Discounts__c;

import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Selenide.$$;

/**
 * Item in the 'License Requirements' section on the {@link DealQualificationPage}.
 * <br/>
 * When there are licenses/products that have discounts on {@link CartPage}, and a user clicks 'Submit for Approval' button,
 * these items are created.
 * <br/>
 * Item's values are linked to the {@link DQ_Deal_Qualification_Discounts__c} object.
 * <br/>
 * It's represented as a single row with the corresponding service, package name, license name, discount value, etc.
 */
public class LicenseRequirementsItem {
    private final SelenideElement container;

    /**
     * Constructor for the License Requirements item that defines its position on the web page.
     *
     * @param licenseName name of the license that corresponds to the given item
     *                    (e.g. 'Polycom VVX 501 Color Touchscreen Phone with 1 Expansion Module').
     */
    public LicenseRequirementsItem(String licenseName) {
        this.container = $$("[forid='license'] span")
                .findBy(exactTextCaseSensitive(licenseName))
                .ancestor("tr");
    }

    /**
     * Get an actual web element behind License Requirements item.
     */
    public SelenideElement getSelf() {
        return this.container;
    }

    /**
     * Get a value of the 'Service' column of the item (e.g. 'Office').
     */
    public SelenideElement getService() {
        return container.$("[forid='service'] span");
    }

    /**
     * Get a value of the 'Package' column of the item (e.g. 'RingEX Core™').
     */
    public SelenideElement getPackage() {
        return container.$("[forid='package'] span");
    }

    /**
     * Get a value of the 'Group' column of the item (e.g. 'Services').
     */
    public SelenideElement getGroup() {
        return container.$("[forid='group'] span");
    }

    /**
     * Get a value of the 'Subgroup' column of the item (e.g. 'Main').
     */
    public SelenideElement getSubGroup() {
        return container.$("[forid='subGroup'] span");
    }

    /**
     * Get an input field for the 'Quantity' column of the item.
     */
    public SelenideElement getQuantityInput() {
        return container.$("#quantity");
    }

    /**
     * Get an input field for the 'Ceiling Discount' column of the item.
     */
    public SelenideElement getCeilingDiscountInput() {
        return container.$("#discount");
    }
}
