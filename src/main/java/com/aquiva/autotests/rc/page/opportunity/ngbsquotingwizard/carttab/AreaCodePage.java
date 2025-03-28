package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab;

import com.aquiva.autotests.rc.model.ngbs.testdata.AreaCode;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

/**
 * Modal window for assigning Area Codes to Quote Line Items.
 * Opens up on top of {@link CartPage} of {@link NGBSQuotingWizardPage}
 * by clicking on area code assignment button for a given Quote Line Item.
 * <p>
 * Typically consists of:
 * <p> - blocks with {@link AreaCodeLineItem} elements. Usually there's only one.
 * See {@link #assignAreaCode} method to add the Area Code.
 * <p> - action buttons "Apply", "Cancel" (and "Add Area Code" button if there are none yet).
 */
public class AreaCodePage {
    public final SelenideElement addAreaCodeButton = $(byText("Add Area Code"));
    public final SelenideElement cancelButton = $("[data-ui-auto='area-code-cancel-button']");
    public final SelenideElement applyButton = $(byText("Apply"));
    public final ElementsCollection areaCodeLineItems = $$("[formarrayname='areaCodesArray'] > div.ng-untouched");

    /**
     * Get the first area code line item block on the page.
     * Usually, this is the area code item that you need in your test.
     *
     * @return AreaCodeLineItem block for a device
     */
    public AreaCodeLineItem getFirstAreaCodeItem() {
        return getAreaCodeLineItem(0);
    }

    /**
     * Get the N-th area code line item block on the page.
     *
     * @param index any non-negative index (start at 0 and more)
     * @return AreaCodeLineItem block for a device
     */
    public AreaCodeLineItem getAreaCodeLineItem(int index) {
        return new AreaCodeLineItem(areaCodeLineItems.get(index));
    }

    /**
     * Assign the Area Code to the selected Quote Line Item.
     * (e.g. Main Local Number, Main Toll-Free Number, Additional Local Number, etc...).
     *
     * @param areaCode Area Code entity with the name of the country, state, city, and the code to assign to the line
     * @param quantity quantity on Area Code Line Item to assign to the Quote Line Item
     */
    public void assignAreaCode(AreaCode areaCode, int quantity) {
        addAreaCodeButton.click();

        var areaCodeLineItem = getFirstAreaCodeItem();
        areaCodeLineItem.setAreaCode(areaCode);
        areaCodeLineItem.getQuantityInput().setValue(String.valueOf(quantity));

        applyButton.click();
    }
}
