package com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.carttab;

import com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.*;
import com.codeborne.selenide.ElementsCollection;

import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.$x;
import static java.time.Duration.ofSeconds;

/**
 * 'Cart' page: one of the tabs on the Legacy Quote Wizard pipeline.
 * <br/><br/>
 * Can be accessed via Legacy Quote Wizard on the 'Main Quote', 'Contact Center' and 'ProServ Quote' tabs.
 * <br/><br/>
 * Contains items that were added on the 'Products' tab with their prices, discounts,
 * and other information.
 *
 * @see BaseLegacyQuotingWizardPage
 * @see ContactCenterQuotingWizardPage
 * @see ProServQuotingWizardPage
 */
public class LegacyCartPage extends BaseLegacyQuotingWizardPage {
    public final ElementsCollection quoteLineItemListEntries = $$("tr.cQuotingToolCartListEntry");

    /**
     * Get Quote Line Item on the Cart Tab by its name.
     *
     * @param name displayed name of the item (e.g. "Contact Center: Basic Edition Seat")
     * @return composite object to extract other parameters from (quantity, price, discount...)
     */
    public LegacyCartItem getQliFromCart(String name) {
        return getQliFromCart(name, EMPTY_STRING);
    }

    /**
     * Get Quote Line Item on the Cart Tab by its name and its family name.
     *
     * @param name   displayed name of the item (e.g. "Contact Center: Basic Edition Seat")
     * @param family displayed family name of the item (e.g. "CC Service")
     * @return composite object to extract other parameters from (quantity, price, discount...)
     */
    public LegacyCartItem getQliFromCart(String name, String family) {
        return new LegacyCartItem(
                $x("//tr[contains(@class,'cQuotingToolCartListEntry') " +
                        "and ./td[contains(@class,'cart__name') " +
                        "and ./div[contains(.,'" + name + "')] " +
                        "and ./div[contains(.,'" + family + "')]" +
                        "]]"
                )
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void waitUntilLoaded() {
        super.waitUntilLoaded();
        quoteLineItemListEntries.shouldHave(sizeGreaterThan(0), ofSeconds(90));
    }

    /**
     * Open the Cart tab by clicking on the tab's button.
     * <br/>
     * Note: there should be at least one product added to the cart before opening the tab!
     */
    public LegacyCartPage openTab() {
        cartTabButton.click();
        waitUntilLoaded();
        return this;
    }
}
