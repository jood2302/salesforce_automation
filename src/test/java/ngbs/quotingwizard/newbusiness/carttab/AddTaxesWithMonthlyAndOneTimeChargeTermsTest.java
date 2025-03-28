package ngbs.quotingwizard.newbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartItem;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.QuoteLineItem;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.cartPage;
import static base.Pages.wizardPage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.Product2Helper.FAMILY_TAXES;
import static com.codeborne.selenide.CollectionCondition.containExactTextsCaseSensitive;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Selenide.$$;
import static io.qameta.allure.Allure.step;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("PDV")
@Tag("NGBS")
@Tag("Taxes")
public class AddTaxesWithMonthlyAndOneTimeChargeTermsTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final String monthlyChargeTerm;
    private final String oneTimeChargeTerm;

    public AddTaxesWithMonthlyAndOneTimeChargeTermsTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_NonContract_1TypeOfDL.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        monthlyChargeTerm = data.getProductByDataName("LC_DL-UNL_50").chargeTerm;
        oneTimeChargeTerm = data.getProductByDataName("LC_HD_959").chargeTerm;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-13221")
    @TmsLink("CRM-13222")
    @DisplayName("CRM-13221 - Adding Taxes return Recurring Part for Recurring Items. \n" +
            "CRM-13222 - Adding Taxes return One-Time Part for One-Time Items.")
    @Description("CRM-13221 - Check that the User receives Recurring (Monthly/Annual charge terms) Taxes for recurring products. \n" +
            "CRM-13222 - Check that the User receives One-Time Taxes for One-Time products.")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () -> {
            steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId());
        });

        step("2. Open the Add Products tab, and add Products with 'One-Time' and 'Monthly' charge terms", () -> {
            steps.quoteWizard.addProductsOnProductsTab(data.getNewProductsToAdd());
        });

        step("3. Open the Price tab, click on 'Add Taxes' button, check that taxes were added in the Cart, and save changes", () -> {
            cartPage.openTab();
            cartPage.addTaxes();
            cartPage.taxCartItems.shouldHave(sizeGreaterThan(0));

            cartPage.saveChanges();
        });

        // For CRM-13221 & CRM-13222
        step("4. Check that Taxes with 'Monthly' and 'One-Time' charge terms were added", () -> {
            step("Check that Taxes with 'Monthly' and 'One-Time' charge terms were added in the Cart", () -> {
                var taxItemsChargeTermsUI = cartPage.getAllTaxCartItems()
                        .stream()
                        .map(CartItem::getChargeTerm)
                        .collect(toList());
                $$(taxItemsChargeTermsUI).should(containExactTextsCaseSensitive(oneTimeChargeTerm, monthlyChargeTerm));
            });

            step("Check that Taxes with 'Monthly' and 'One-Time' charge terms were added in the database", () -> {
                var taxItems = enterpriseConnectionUtils.query(
                        "SELECT Id, ChargeTerm__c " +
                                "FROM QuoteLineItem " +
                                "WHERE QuoteId = '" + wizardPage.getSelectedQuoteId() + "' " +
                                "AND Product2.Family = '" + FAMILY_TAXES + "'",
                        QuoteLineItem.class);
                var taxItemsChargeTermsDB = taxItems
                        .stream()
                        .map(QuoteLineItem::getChargeTerm__c)
                        .collect(toList());

                assertThat(taxItemsChargeTermsDB)
                        .as("List of ChargeTerm__c values for Taxes QLIs in DB")
                        .contains(monthlyChargeTerm, oneTimeChargeTerm);
            });
        });
    }
}
