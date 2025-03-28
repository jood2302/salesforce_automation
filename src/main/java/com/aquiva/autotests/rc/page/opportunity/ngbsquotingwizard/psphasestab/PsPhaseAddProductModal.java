package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.psphasestab;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selectors.withText;
import static com.codeborne.selenide.Selenide.$;

/**
 * 'Add Product' modal window.
 * <br/>
 * Used to add product to the created Phase on the {@link PsPhasesPage}.
 */
public class PsPhaseAddProductModal {
    //  For the 'Quantity' field
    public static final String DEFAULT_PRODUCT_QUANTITY = "1";

    private final SelenideElement dialogContainer = $("add-product-modal");

    public final SelenideElement productNamePicklist = dialogContainer.$("#product-name");
    public final SelenideElement quantityInput = dialogContainer.$("#product-quantity");
    public final SelenideElement applyButton = dialogContainer.$(withText("Apply"));
}
