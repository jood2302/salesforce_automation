package ngbs.quotingwizard.existingbusiness.quotetab;

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

import static base.Pages.cartPage;
import static base.Pages.quotePage;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.getFreeServiceCreditTotalValueForUsQuotes;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.Product2Helper.FAMILY_TAXES;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("QuoteTab")
public class FullMrsCalculationTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final Product dlUnlimited;
    private final Product polycomPhone;
    private final Product contactCenterInterconnect;
    private final String specialTerms;
    private final int specialTermsMonthsQuantity;

    public FullMrsCalculationTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_Contract_163075013.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        polycomPhone = data.getProductByDataName("LC_HD_687");
        contactCenterInterconnect = data.getProductByDataName("LC_ICI_477");
        specialTermsMonthsQuantity = 2;
        specialTerms = specialTermsMonthsQuantity + " Free Months of Service";
    }

    @BeforeEach
    public void setUpTest() {
        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.generateBillingAccount();
            steps.ngbs.stepCreateContractInNGBS();
        }

        var dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUser);
        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    @Test
    @TmsLink("CRM-22392")
    @DisplayName("CRM-22392 - Full MRS Calculation for Existing Business Upsell with Contract")
    @Description("Check that Full MRS FSC is calculated for the entire cart state if toggle on Quote Details tab is in Full MRS position")
    public void test() {
        step("1. Open the Quote Wizard for the Engage Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        step("2. Add products on the Add Products tab", () ->
                steps.quoteWizard.addProductsOnProductsTab(polycomPhone, contactCenterInterconnect)
        );

        step("3. Open the Price Tab, set up quantities and discounts, assign the phones to DLs, and save changes", () -> {
            cartPage.openTab();
            steps.cartTab.setUpQuantities(dlUnlimited, polycomPhone, contactCenterInterconnect);
            steps.cartTab.setUpDiscounts(dlUnlimited);
            steps.cartTab.assignDevicesToDL(polycomPhone.name, dlUnlimited.name, steps.quoteWizard.localAreaCode,
                    polycomPhone.quantity);
        });

        step("4. Press 'Add Taxes' button", () -> {
            cartPage.addTaxes();
            cartPage.taxCartItems.shouldHave(sizeGreaterThan(0));
        });

        step("5. Open the Quote Details tab, populate required fields, " +
                "open the Billing Details and Terms modal, populate Special Terms, switch 'Full MRS' toggle on, and save changes", () -> {
            quotePage.openTab();
            quotePage.discountJustificationTextArea.setValue(TEST_STRING);
            quotePage.footer.billingDetailsAndTermsButton.click();

            quotePage.billingDetailsAndTermsModal.specialTermsPicklist.shouldBe(visible, ofSeconds(30)).selectOption(specialTerms);
            quotePage.billingDetailsAndTermsModal.fullMrsToggle.click();
            quotePage.applyChangesInBillingDetailsAndTermsModal();
            quotePage.saveChanges();
        });

        step("6. Check the calculated amount of Free Service Credit (on the Quote Details tab and in the DB)", () -> {
            var quote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Free_Service_Taxes__c, Free_Service_Credit_Total__c " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "'",
                    Quote.class);

            var oneTimeChargeTerm = polycomPhone.chargeTerm;
            var quoteLineItems = enterpriseConnectionUtils.query(
                    "SELECT NewQuantity__c, EffectivePriceNew__c " +
                            "FROM QuoteLineItem " +
                            "WHERE Quote.OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "' " +
                            "AND Product2.Family != '" + FAMILY_TAXES + "' " +
                            "AND ChargeTerm__c != '" + oneTimeChargeTerm + "'",
                    QuoteLineItem.class);

            var qliFreeServiceCreditSum = quoteLineItems.stream()
                    .mapToDouble(qli -> qli.getNewQuantity__c() * qli.getEffectivePriceNew__c() * specialTermsMonthsQuantity)
                    .sum();
            var expectedFreeServiceCreditTotal = quote.getFree_Service_Taxes__c() + qliFreeServiceCreditSum;

            assertThat(quote.getFree_Service_Credit_Total__c())
                    .as("Quote.Free_Service_Credit_Total__c value")
                    .isEqualTo(expectedFreeServiceCreditTotal);

            var expectedFreeServiceCreditTotalFormatted = getFreeServiceCreditTotalValueForUsQuotes(expectedFreeServiceCreditTotal);
            quotePage.freeServiceCreditAmount.shouldHave(exactTextCaseSensitive(expectedFreeServiceCreditTotalFormatted));
        });
    }
}
