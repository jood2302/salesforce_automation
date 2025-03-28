package ngbs.quotingwizard.newbusiness.quotetab.specialterms;

import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import com.sforce.soap.enterprise.sobject.QuoteLineItem;
import ngbs.quotingwizard.QuoteWizardSteps;

import java.math.BigDecimal;
import java.util.List;

import static base.Pages.cartPage;
import static base.Pages.quotePage;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.getFreeServiceCreditTotalValueForUsQuotes;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.aquiva.autotests.rc.utilities.TimeoutAssertions.assertWithTimeout;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.Product2Helper.FAMILY_TAXES;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteLineItemHelper.MONTHLY_CHARGE_TERM;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteLineItemHelper.ONE_TIME_BCD;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;
import static java.math.RoundingMode.CEILING;
import static java.math.RoundingMode.FLOOR;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test methods for the test cases related to 'Free Service Credit', 'Free Service Taxes'
 * and 'Credit Amount' fields checks using different Special Terms.
 */
public class SpecialTermsSteps {
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;
    private final QuoteWizardSteps quoteWizardSteps;

    private BigDecimal freeServiceCreditTotalExpected;

    //  Test data
    private final List<Integer> numberOfFreeMonths;
    private final int scale;
    private final int monthsInYear;
    private final boolean isMonthly;
    private final AreaCode localAreaCode;

    /**
     * New instance for the class with the test methods/steps
     * for the test cases related to 'Free Service Credit', 'Free Service Taxes'
     * and 'Credit Amount' fields checks using different Special Terms.
     *
     * @param data object parsed from the JSON files with the test data
     */
    public SpecialTermsSteps(Dataset data) {
        quoteWizardSteps = new QuoteWizardSteps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        numberOfFreeMonths = List.of(1, 3, 6);
        scale = 2;
        monthsInYear = 12;
        isMonthly = data.chargeTerm.equalsIgnoreCase(MONTHLY_CHARGE_TERM);

        localAreaCode = new AreaCode("Local", "United States", "California", EMPTY_STRING, "619");
    }

    /**
     * Test steps related to test cases that check that 'Credit Amount' is calculated correctly for different 'Special Terms',
     * and the calculation doesn't count Total price of One-Time items on the Quote.
     */
    public void creditAmountTestSteps(String opportunityId, Product productToAdd) {
        step("1. Open the Quote Wizard for a New Business Opportunity to add a new Sales quote, " +
                "select a package for it, and save changes", () ->
                quoteWizardSteps.prepareOpportunityViaQuoteWizardVfPage(opportunityId)
        );

        step("2. Open the Add Products tab and add some products", () -> {
            quoteWizardSteps.addProductsOnProductsTab(productToAdd);
        });

        step("3. Switch to the Quote Details tab, and populate Main Area Code field", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(localAreaCode);
        });

        step("4. Check 'Credit Amount' with several options of 'Special Term' picklist", () -> {
            var quoteId = quotePage.getSelectedQuoteId();

            for (var monthsNumber : numberOfFreeMonths) {
                step("Check that 'Credit Amount' is calculated correctly " +
                        "with Number of Months (Free Service Credit) = '" + monthsNumber + " Free Month(s) of Service'", () -> {
                    var specialTermValue = monthsNumber + " Free Month";

                    quotePage.applyNewSpecialTerms(specialTermValue);

                    assertWithTimeout(() -> {
                        var quote = enterpriseConnectionUtils.querySingleRecord(
                                "SELECT Id, TotalPrice, Total_One_Time__c, Credit_Amount__c " +
                                        "FROM Quote " +
                                        "WHERE Id = '" + quoteId + "'",
                                Quote.class);
                        assertNotNull(quote.getTotalPrice(), "Quote.TotalPrice value");
                        assertNotNull(quote.getTotal_One_Time__c(), "Quote.Total_One_Time__c value");
                        assertNotNull(quote.getCredit_Amount__c(), "Quote.Credit_Amount__c value");

                        var totalPriceActual = BigDecimal.valueOf(quote.getTotalPrice())
                                .setScale(scale, CEILING);

                        var oneTimeItemsTotalPriceActual = BigDecimal.valueOf(quote.getTotal_One_Time__c())
                                .setScale(scale, CEILING);

                        var creditAmountActual = BigDecimal.valueOf(quote.getCredit_Amount__c())
                                .setScale(scale, CEILING);

                        var recurringCharges = totalPriceActual.subtract(oneTimeItemsTotalPriceActual);
                        var creditAmountExpected = isMonthly ?
                                recurringCharges.multiply(new BigDecimal(monthsNumber)) :
                                recurringCharges.multiply(new BigDecimal(monthsNumber)).divide(new BigDecimal(monthsInYear), scale, CEILING);
                        assertEquals(creditAmountExpected, creditAmountActual, "Quote.Credit_Amount__c value");
                    }, ofSeconds(10));
                });
            }
        });
    }

    /**
     * Test steps related to test cases that check that 'Free Service Credit' and
     * 'Free Service Taxes' are calculated correctly for different 'Special Terms' values.
     */
    public void freeServiceCreditAndFreeServiceTaxesTestSteps(String opportunityId) {
        step("1. Open the Quote Wizard for the New Business Opportunity to add a new Sales quote, " +
                "select a package for it, save changes", () -> {
            quoteWizardSteps.prepareOpportunityViaQuoteWizardVfPage(opportunityId);
        });

        step("2. Open the Price tab, and add Taxes on it", () -> {
            cartPage.openTab();
            cartPage.addTaxes();
            cartPage.taxCartItems.shouldHave(sizeGreaterThan(0));
        });

        step("3. Switch to the Quote Details tab, set Main Area Code field, and save changes", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(localAreaCode);
            //  Additional save to easily retrieve the Tax QLIs from the backend once for all the different values of Special Term in the next step
            quotePage.saveChanges();
        });

        step("4. Check 'Free Service Credit' and 'Free Service Taxes' with several variants of 'Special Term'", () -> {
            var quoteId = quotePage.getSelectedQuoteId();

            var taxesForCalculating = enterpriseConnectionUtils.query(
                    "SELECT Id, EffectivePriceNew__c " +
                            "FROM QuoteLineItem " +
                            "WHERE QuoteId = '" + quoteId + "' " +
                            "AND Billing_Cycle_Duration__c != '" + ONE_TIME_BCD + "' " +
                            "AND Product2.Family = '" + FAMILY_TAXES + "'",
                    QuoteLineItem.class);
            assertThat(taxesForCalculating)
                    .as("QuoteLineItems for Taxes (all EffectivePriceNew__c fields should not be null)")
                    .allMatch(qli -> qli.getEffectivePriceNew__c() != null);

            var sumOfTaxes = BigDecimal.valueOf(
                    taxesForCalculating.stream()
                            .mapToDouble(QuoteLineItem::getEffectivePriceNew__c)
                            .sum());

            for (var monthsNumber : numberOfFreeMonths) {
                step("Check 'Free Service Taxes' and 'Free Service Credit' " +
                        "with Number of Months (Free Service Credit) = '" + monthsNumber + " Free Month(s) of Service'", () -> {
                    var specialTermValue = monthsNumber + " Free Month";

                    quotePage.applyNewSpecialTerms(specialTermValue);

                    assertWithTimeout(() -> {
                        var quote = enterpriseConnectionUtils.querySingleRecord(
                                "SELECT Id, Credit_Amount__c, Free_Service_Taxes__c, Free_Service_Credit_Total__c " +
                                        "FROM Quote " +
                                        "WHERE Id = '" + quoteId + "'",
                                Quote.class);
                        assertNotNull(quote.getCredit_Amount__c(), "Quote.Credit_Amount__c value");
                        assertNotNull(quote.getFree_Service_Taxes__c(), "Quote.Free_Service_Taxes__c value");
                        assertNotNull(quote.getFree_Service_Credit_Total__c(), "Quote.Free_Service_Credit_Total__c value");

                        var creditAmountActual = BigDecimal.valueOf(quote.getCredit_Amount__c())
                                .setScale(scale, CEILING);

                        var freeServiceTaxesActual = BigDecimal.valueOf(quote.getFree_Service_Taxes__c())
                                .setScale(scale, FLOOR);

                        var freeServiceCreditTotalActual = BigDecimal.valueOf(quote.getFree_Service_Credit_Total__c())
                                .setScale(scale, CEILING);

                        var freeServiceTaxesExpected = isMonthly ?
                                sumOfTaxes.multiply(new BigDecimal(monthsNumber)).setScale(scale, FLOOR) :
                                sumOfTaxes.multiply(new BigDecimal(monthsNumber)).divide(BigDecimal.valueOf(monthsInYear), scale, FLOOR);
                        assertEquals(freeServiceTaxesExpected, freeServiceTaxesActual,
                                "Quote.Free_Service_Taxes__c value");

                        freeServiceCreditTotalExpected = freeServiceTaxesActual.add(creditAmountActual);
                        assertEquals(freeServiceCreditTotalExpected, freeServiceCreditTotalActual,
                                "Quote.Free_Service_Credit_Total__c value");
                    }, ofSeconds(10));

                    var expectedFreeServiceCreditTotalFormatted =
                            getFreeServiceCreditTotalValueForUsQuotes(freeServiceCreditTotalExpected.doubleValue());
                    quotePage.freeServiceCreditAmount.shouldHave(exactTextCaseSensitive(expectedFreeServiceCreditTotalFormatted));
                });
            }
        });
    }
}
