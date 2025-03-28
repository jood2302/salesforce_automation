package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal;

import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartPage;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;

/**
 * Modal window in {@link CartPage} activated by clicking on Target Price of the Cart Item.
 * <p>
 * This modal window contains 'Recommended discount' value for a selected Cart Item
 * that can be accepted with 'Accept Recommendation' button.
 * </p>
 */
public class TargetPriceDetailsModal {
    private final SelenideElement dialogContainer = $("target-price-details-modal");

    /**
     * Get Recommended Discount value for the provided Product.
     *
     * @param productName name of the provided Product
     * @return web element that contains Recommended Discount value for the provided Product
     */
    public SelenideElement getRecommendedDiscount(String productName) {
        //  TODO Add unique attribute kind of 'data-ui-auto' for 'Recommended Discount' cell
        return dialogContainer.$x(".//div[contains(text(),'" + productName + "')]/following-sibling::div/div[2]/div[2]/div");
    }

    //  Buttons
    public final SelenideElement acceptRecommendationsButton = dialogContainer.$(byText("Accept Recommendations"));
}
