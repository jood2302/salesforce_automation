package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NgbsQuotingWizardFooter.CONTRACT_ACTIVE;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NgbsQuotingWizardFooter.CONTRACT_NONE;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.math.RoundingMode.DOWN;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("QTFooter")
@Tag("UQT")
public class GlobalFooterUpdatesFromQuoteDataTest extends BaseTest {
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
    private final Integer digitalLinesTotalQuantity;

    private final String initialTerm;
    private final String renewalTerm;
    private final String monthlyChargeTerm;
    private final String annualChargeTerm;
    private final String packageFolderName;
    private final Package packageToSelect;

    public GlobalFooterUpdatesFromQuoteDataTest() {
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
        polycomOneTimePhone = data.getProductByDataName("LC_HD_687");
        digitalLinesTotalQuantity = 3; //   DL Unlimited + DL Basic + Common Phone quantities

        initialTerm = data.packageFolders[0].packages[0].contractTerms.initialTerm[0];
        renewalTerm = data.packageFolders[0].packages[0].contractTerms.renewalTerm;
        monthlyChargeTerm = "Monthly";
        annualChargeTerm = data.chargeTerm;
        packageFolderName = data.packageFolders[0].name;
        packageToSelect = data.packageFolders[0].packages[0];
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-26119")
    @DisplayName("CRM-26119 - Displaying Quote data in Global Footer")
    @Description("Verify that Quote data (Charge Term / Contract / Contract Terms / Price totals) are updated in Global Footer " +
            "after they are changed in Quote Wizard")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package with Monthly charge term and without Contract", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
            packagePage.packageSelector.selectPackage(monthlyChargeTerm, packageFolderName, packageToSelect);
            packagePage.packageSelector.setContractSelected(false);
        });

        step("2. Open to the Add Products tab, and check the Footer's fields", () -> {
            productsPage.openTab();

            productsPage.footer.footerContainer.shouldBe(visible);
            productsPage.footer.paymentPlan.shouldHave(exactTextCaseSensitive(monthlyChargeTerm));
            productsPage.footer.contract.shouldHave(exactTextCaseSensitive(CONTRACT_NONE));
            productsPage.footer.initialTerm.shouldBe(hidden);
            productsPage.footer.renewalTerm.shouldBe(hidden);
            productsPage.footer.freeServiceCredit.shouldBe(hidden);
            productsPage.footer.specialShippingTerms.shouldBe(hidden);
        });

        step("3. Open the Select Package tab, set the Charge Term = 'Annual', check the Contract checkbox, " +
                "open the Add Products tab, and check the Contract and Payment Plan values in the Footer", () -> {
            packagePage.openTab();
            packagePage.packageSelector.packageFilter.selectChargeTerm(annualChargeTerm);
            packagePage.packageSelector.setContractSelected(true);

            productsPage.openTab();
            productsPage.footer.paymentPlan.shouldHave(exactTextCaseSensitive(annualChargeTerm));
            productsPage.footer.contract.shouldHave(exactTextCaseSensitive(CONTRACT_ACTIVE));
        });

        step("4. Add the One-Time and Recurring products to the Cart, open the Price tab, and save changes", () -> {
            steps.quoteWizard.addProductsOnProductsTab(data.getNewProductsToAdd());
            productsPage.addProduct(polycomOneTimePhone);

            cartPage.openTab();
            cartPage.saveChanges();
        });

        step("5. Check the total Cost of One-Time items in the Footer on the Price and Quote Details tabs, " +
                "and in the database", () -> {
            var costOfOneTimeItemsExpected = new BigDecimal(ciscoPhone.price)
                    .add(new BigDecimal(polycomOneTimePhone.price))
                    .setScale(2, DOWN)
                    .toString();

            cartPage.footer.costOfOneTimeItems.shouldHave(exactText(costOfOneTimeItemsExpected));
            quotePage.openTab();
            quotePage.footer.costOfOneTimeItems.shouldHave(exactText(costOfOneTimeItemsExpected));

            var quote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Total_One_Time__c " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "'",
                    Quote.class);
            assertThat(quote.getTotal_One_Time__c())
                    .as("Quote.Total_One_Time__c value")
                    .isEqualTo(Double.valueOf(costOfOneTimeItemsExpected));
        });

        step("6. Check the New Annual Recurring Charges in the Footer on the Price and Quote Details tabs, " +
                "and in the database", () -> {
            var dlUnlimitedPriceExpected = new BigDecimal(digitalLineUnlimited.price);
            var dlBasicPriceExpected = new BigDecimal(digitalLineBasic.price);
            var addLocalNumberTotalPriceExpected = new BigDecimal(addLocalNumber.price);
            var globalMvpEmeaPriceExpected = new BigDecimal(globalMvpEMEA.price);
            var polycomRentalPhonePriceExpected = new BigDecimal(polycomRentalPhone.price);
            var e911ServiceTotalPriceExpected = new BigDecimal(e911ServiceFee.price)
                    .multiply(BigDecimal.valueOf(digitalLinesTotalQuantity));
            var complianceFeeTotalPriceExpected = new BigDecimal(complianceFee.price)
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

            quotePage.footer.newRecurringCharges.shouldHave(exactText(annualRecurringRevenueExpected));
            cartPage.openTab();
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

        step("7. Open the Quote Details tab, set Initial Term, Renewal Term, Start Date, Main Area Code, " +
                "and check Initial Term and Renewal Term values in the Footer", () -> {
            quotePage.openTab();
            quotePage.initialTermPicklist.selectOption(initialTerm);
            quotePage.renewalTermPicklist.selectOption(renewalTerm);
            quotePage.setDefaultStartDate();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);

            quotePage.footer.initialTerm.shouldHave(exactText(initialTerm));
            quotePage.footer.renewalTerm.shouldHave(exactText(renewalTerm));
        });
    }
}
