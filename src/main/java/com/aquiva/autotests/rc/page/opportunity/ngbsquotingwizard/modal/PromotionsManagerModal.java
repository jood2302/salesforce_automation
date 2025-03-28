package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal;

import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartPage;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static java.time.Duration.ofSeconds;

/**
 * Modal window in {@link NGBSQuotingWizardPage}
 * activated by clicking on "Promos" button on the {@link CartPage}.
 * <br/>
 * This dialog manages promos for products or categories of products.
 */
public class PromotionsManagerModal {
    private final SelenideElement dialogContainer = $("promo-modal");

    //  Buttons
    public final SelenideElement cancelButton = dialogContainer.$(byText("Cancel"));
    public final SelenideElement submitButton = dialogContainer.$(byText("Submit"));
    public final SelenideElement removeButton = dialogContainer.$(byText("Remove"));

    /**
     * Click on 'Apply' button for the current promo.
     *
     * @param promoCode Promo Code to be applied (e.g. "QA-AUTO-DL-USD", "NYPROMO2").
     */
    public void clickApplyPromoButton(String promoCode) {
        var promoRow = dialogContainer
                .$x(".//tbody[.//*[@data-label='Promo Code'][.//*[@title='" + promoCode + "']]]");
        promoRow.shouldBe(visible, ofSeconds(20))
                .$(byText("Apply")).click();
    }
}
