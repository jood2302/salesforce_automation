package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import com.sforce.soap.enterprise.sobject.QuoteLineItem;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartPage.NOT_REQUIRED_APPROVAL_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.Product2Helper.FAMILY_TAXES;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteLineItemHelper.MONTHLY_CHARGE_TERM;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.lang.Double.parseDouble;
import static java.math.RoundingMode.DOWN;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("QTFooter")
@Tag("UQT")
public class ChangeChargeTermInGlobalFooterTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final Product addLocalNumber;
    private final Product complianceFee;
    private final Product e911ServiceFee;
    private final Product digitalLineUnlimited;
    private final Product globalMvpEMEA;
    private final Product digitalLineBasic;
    private final Product polycomRentalPhone;
    private final Product ciscoPhone;
    private final Product polycomOneTimePhone;
    private final int digitalLinesTotalQuantity;
    private final Product[] recurringItemsWithNonZeroPrice;

    public ChangeChargeTermInGlobalFooterTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Annual_Contract_PhonesAndDLsAndGlobalMVP.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        addLocalNumber = data.getProductByDataName("LC_ALN_38");
        complianceFee = data.getProductByDataName("LC_CRF_51");
        e911ServiceFee = data.getProductByDataName("LC_E911_52");
        digitalLineUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        globalMvpEMEA = data.getProductByDataName("LC_IBO_284");
        digitalLineBasic = data.getProductByDataName("LC_DL-BAS_178");
        polycomRentalPhone = data.getProductByDataName("LC_HDR_619");
        ciscoPhone = data.getProductByDataName("LC_HD_523");
        ciscoPhone.discount = 20;
        ciscoPhone.yourPrice = "277.60"; // 347 USD * (1 - 0.2) = 277.60
        polycomOneTimePhone = data.getProductByDataName("LC_HD_687");
        digitalLinesTotalQuantity = 3; //   DL Unlimited + DL Basic + Common Phone quantities

        recurringItemsWithNonZeroPrice = new Product[]{addLocalNumber, e911ServiceFee, complianceFee,
                digitalLineUnlimited, digitalLineBasic, globalMvpEMEA, polycomRentalPhone};
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-26124")
    @DisplayName("CRM-26124 - Changing Charge Term in Global Footer")
    @Description("Verify that Charge Term (Annual/Monthly) can be changed in the Footer of Quote Wizard " +
            "and the Quote is updated accordingly")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, and select package for it", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.selectDefaultPackageFromTestData();
        });

        step("2. Open the Add Products tab, and add One-Time and Recurring items", () -> {
            steps.quoteWizard.addProductsOnProductsTab(data.getNewProductsToAdd());
            productsPage.addProduct(polycomOneTimePhone);
        });

        step("3. Open the Price tab, set up discounts for DL Unlimited, DL Basic, Global MVP - EMEA and phone, " +
                "save changes, add taxes, and save changes again", () -> {
            cartPage.openTab();
            steps.cartTab.setUpDiscounts(digitalLineBasic, digitalLineUnlimited, globalMvpEMEA, ciscoPhone);
            cartPage.saveChanges();

            cartPage.addTaxes();
            cartPage.taxCartItems.shouldHave(sizeGreaterThan(0));
            cartPage.saveChanges();
        });

        step("4. Approve the current Quote via API", () ->
                steps.quoteWizard.stepUpdateQuoteToApprovedStatus(steps.quoteWizard.opportunity.getId())
        );

        step("5. Check the Payment Plan field in the Footer", () -> {
            cartPage.footer.paymentPlan.shouldHave(exactTextCaseSensitive(data.chargeTerm));
        });

        step("6. Check the total Cost of One-Time items in the Footer and in the database", () -> {
            var costOfOneTimeItemsExpected = new BigDecimal(ciscoPhone.yourPrice)
                    .add(new BigDecimal(polycomOneTimePhone.yourPrice))
                    .setScale(2, DOWN)
                    .toString();

            cartPage.footer.costOfOneTimeItems.shouldHave(exactText(costOfOneTimeItemsExpected));

            var quote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Total_One_Time__c " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "'",
                    Quote.class);
            assertThat(quote.getTotal_One_Time__c())
                    .as("Quote.Total_One_Time__c value")
                    .isEqualTo(Double.valueOf(costOfOneTimeItemsExpected));
        });

        step("7. Check the New Annual Recurring Charges in the Footer and in the database", () -> {
            var dlUnlimitedPriceExpected = new BigDecimal(digitalLineUnlimited.yourPrice);
            var dlBasicPriceExpected = new BigDecimal(digitalLineBasic.yourPrice);
            var addLocalNumberTotalPriceExpected = new BigDecimal(addLocalNumber.yourPrice);
            var globalMvpEmeaPriceExpected = new BigDecimal(globalMvpEMEA.yourPrice);
            var polycomRentalPhonePriceExpected = new BigDecimal(polycomRentalPhone.yourPrice);
            var e911ServiceTotalPriceExpected = new BigDecimal(e911ServiceFee.yourPrice)
                    .multiply(BigDecimal.valueOf(digitalLinesTotalQuantity));
            var complianceFeeTotalPriceExpected = new BigDecimal(complianceFee.yourPrice)
                    .multiply(BigDecimal.valueOf(digitalLinesTotalQuantity));

            var annualRecurringRevenueExpected = dlUnlimitedPriceExpected
                    .add(dlBasicPriceExpected)
                    .add(addLocalNumberTotalPriceExpected)
                    .add(globalMvpEmeaPriceExpected)
                    .add(polycomRentalPhonePriceExpected)
                    .add(e911ServiceTotalPriceExpected)
                    .add(complianceFeeTotalPriceExpected)
                    .setScale(2, DOWN)
                    .toString();

            cartPage.footer.newRecurringCharges.shouldHave(exactText(annualRecurringRevenueExpected));

            var quote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Total_ARR__c " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "'",
                    Quote.class);
            assertThat(quote.getTotal_ARR__c())
                    .as("Quote.Total_ARR__c value")
                    .isEqualTo(Double.valueOf(annualRecurringRevenueExpected));
        });

        step("8. Open the Add Products tab, click 'Billing Details and Terms' button in the Footer, " +
                "change Payment Plan to 'Monthly' in the modal window, and apply changes", () -> {
            productsPage.openTab();

            productsPage.footer.billingDetailsAndTermsButton.click();
            productsPage.billingDetailsAndTermsModal.selectChargeTerm(MONTHLY_CHARGE_TERM);
            productsPage.applyChangesInBillingDetailsAndTermsModal();

            productsPage.footer.paymentPlan.shouldHave(exactTextCaseSensitive(MONTHLY_CHARGE_TERM));
        });

        step("9. Open the Price tab, save changes on it, " +
                "and verify that Charge Term of recurring items are changed to 'Monthly', " +
                "their prices are updated, and all discounts are removed", () -> {
            cartPage.openTab();
            cartPage.saveChanges();

            for (var product : recurringItemsWithNonZeroPrice) {
                step("Check the List Price, Discount, and Charge Term on the Price tab for " + product.name, () -> {
                    var cartItem = cartPage.getQliFromCartByDisplayName(product.name);
                    cartItem.getListPrice().shouldNotHave(exactText(product.price));
                    cartItem.getDiscountInput().shouldHave(exactValue("0"));
                    cartItem.getChargeTerm().shouldHave(exactTextCaseSensitive(MONTHLY_CHARGE_TERM));
                });

                step("Check UnitPrice, ChargeTerm__c, Discount_number__c on the QuoteLineItem record for " + product.name, () -> {
                    var qli = enterpriseConnectionUtils.querySingleRecord(
                            "SELECT Id, UnitPrice, Discount_number__c, ChargeTerm__c " +
                                    "FROM QuoteLineItem " +
                                    "WHERE QuoteId = '" + wizardPage.getSelectedQuoteId() + "' " +
                                    "AND Product2.ExtID__c = '" + product.dataName + "'",
                            QuoteLineItem.class);

                    assertThat(qli.getUnitPrice())
                            .as("QuoteLineItem.UnitPrice value of %s", product.dataName)
                            .isNotEqualTo(parseDouble(product.price));
                    assertThat(qli.getDiscount_number__c())
                            .as("QuoteLineItem.Discount_number__c value of %s", product.dataName)
                            .isEqualTo(0);
                    assertThat(qli.getChargeTerm__c())
                            .as("QuoteLineItem.ChargeTerm__c value of %s", qli.getDisplay_Name__c())
                            .isEqualTo(MONTHLY_CHARGE_TERM);
                });
            }

            cartPage.getQliFromCartByDisplayName(ciscoPhone.name).getDiscountInput().shouldHave(exactValue("0"));
        });

        step("10. Verify that taxes are removed on the Price tab and in the DB", () -> {
            cartPage.taxCartItems.shouldHave(size(0));

            var taxItems = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM QuoteLineItem " +
                            "WHERE QuoteId = '" + wizardPage.getSelectedQuoteId() + "' " +
                            "AND Product2.Family = '" + FAMILY_TAXES + "'",
                    QuoteLineItem.class);
            assertThat(taxItems)
                    .as("List of Taxes QLIs in DB")
                    .isEmpty();
        });

        step("11. Check that the Approval Status = 'Not Required'", () -> {
            cartPage.approvalStatus.shouldHave(exactTextCaseSensitive(NOT_REQUIRED_APPROVAL_STATUS));
        });
    }
}
