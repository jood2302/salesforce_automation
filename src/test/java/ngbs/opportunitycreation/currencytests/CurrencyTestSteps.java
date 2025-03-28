package ngbs.opportunitycreation.currencytests;

import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Opportunity;
import com.sforce.ws.ConnectionException;

import static base.Pages.cartPage;
import static base.Pages.opportunityPage;
import static com.codeborne.selenide.Condition.text;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test methods and steps for test cases that create opportunities via Quick Opportunity Page
 * and check CurrencyIsoCode's on Opportunities and on Products related to that Opportunities.
 */
public class CurrencyTestSteps {
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final String currencyIsoCode;

    /**
     * New instance for the class with test methods for checking CurrencyIsoCode
     * on Opportunities and related Products.
     *
     * @param currencyIsoCode ISO code for account's currency (e.g. "USD", "EUR", etc...)
     */
    public CurrencyTestSteps(String currencyIsoCode) {
        this.currencyIsoCode = currencyIsoCode;

        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    /**
     * Check the Opportunity.CurrencyIsoCode on the currently opened Opportunity.
     *
     * @throws ConnectionException in case of errors while accessing API
     */
    public void checkCurrencyIsoCodeOnOpportunity() throws ConnectionException {
        var opportunity = enterpriseConnectionUtils.querySingleRecord(
                "SELECT Id, CurrencyIsoCode " +
                        "FROM Opportunity " +
                        "WHERE Id = '" + opportunityPage.getCurrentRecordId() + "'",
                Opportunity.class);
        assertThat(opportunity.getCurrencyIsoCode())
                .as("Opportunity.CurrencyIsoCode value")
                .isEqualTo(currencyIsoCode);
    }

    /**
     * Open the Price tab, and check that the expected Currency ISO code is displayed on all the items there.
     */
    public void checkCurrencyOnThePriceTab() {
        step("Open the Price tab, " +
                "and check that Currency ISO Code = " + currencyIsoCode + " is used for items in the Cart", () -> {
            cartPage.openTab();

            for (var cartItem : cartPage.getAllVisibleCartItems()) {
                step("Checking the currency for the cart item '" + cartItem.getDisplayName().text() + "'", () -> {
                    cartItem.getListPrice().shouldHave(text(currencyIsoCode));
                    cartItem.getYourPrice().shouldHave(text(currencyIsoCode));
                    cartItem.getTotalPrice().shouldHave(text(currencyIsoCode));
                });
            }
        });
    }
}
