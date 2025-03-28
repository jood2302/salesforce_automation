package ngbs.quotingwizard.newbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Contract_Discount__c;
import com.sforce.soap.enterprise.sobject.QuoteLineItem;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.cartPage;
import static base.Pages.packagePage;
import static com.codeborne.selenide.Condition.exactText;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("P1")
@Tag("PDV")
@Tag("NGBS")
public class ListPriceUpdateWithContractChangeTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final Product dlUnlimitedNoContract;
    private final Product dlUnlimitedWithContract;
    private final String contractExtId;
    private final Double expectedContractDiscountAmount;

    public ListPriceUpdateWithContractChangeTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_NonContract_1TypeOfDL.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        dlUnlimitedNoContract = data.getProductByDataName("LC_DL-UNL_50", data.packageFolders[0].packages[0]);
        dlUnlimitedWithContract = data.getProductByDataName("LC_DL-UNL_50", data.packageFolders[0].packages[2]);
        contractExtId = data.packageFolders[0].packages[2].contractExtId;
        expectedContractDiscountAmount = 5.00;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-7855")
    @TmsLink("CRM-23927")
    @DisplayName("CRM-7855 - New Business. List price with Contract.\n" +
            "CRM-23927 - Prices of the DigitalLine Unlimited are the same on the Price Tab and on its QLI after removing a Contract")
    @Description("CRM-7855 - Verify that List Price displayed on affected Items are discounted with contract's discount. \n" +
            "CRM-23927 - Check that after removing a Contract, prices of the DigitalLine Unlimited are the same on the Price Tab and on its QLI")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, add some products, and save changes on the Price tab", () ->
                steps.cartTab.prepareCartTabViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        step("2. Check the list price of the DL Unlimited", () -> {
            cartPage.getQliFromCartByDisplayName(dlUnlimitedNoContract.name)
                    .getListPrice()
                    .shouldHave(exactText(steps.quoteWizard.currencyPrefix + dlUnlimitedNoContract.price));
        });

        //  For CRM-7855
        step("3. Open the Select Package tab, add a contract for the selected package, and save changes", () -> {
            packagePage.openTab();
            packagePage.packageSelector.setContractSelected(true);
            packagePage.saveChanges();
        });

        //  For CRM-7855
        step("4. Open the Price tab and check the list price of the DL Unlimited", () -> {
            cartPage.openTab();

            cartPage.getQliFromCartByDisplayName(dlUnlimitedWithContract.name)
                    .getListPrice()
                    .shouldHave(exactText(steps.quoteWizard.currencyPrefix + dlUnlimitedWithContract.price));

            var contractDiscountForDlUnlimited = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Discount__c " +
                            "FROM Contract_Discount__c " +
                            "WHERE Contract__r.ExtID__c  = '" + contractExtId + "' " +
                            "AND ApplicableTo__c = '" + dlUnlimitedWithContract.dataName + "'",
                    Contract_Discount__c.class);
            assertThat(contractDiscountForDlUnlimited.getDiscount__c())
                    .as("Contract_Discount__c.Discount__c value")
                    .isEqualTo(expectedContractDiscountAmount);
        });

        //  For CRM-23927
        step("5. Open the Select Package tab, uncheck 'Contract' checkbox, and save changes", () -> {
            packagePage.openTab();

            packagePage.packageSelector.setContractSelected(false);
            packagePage.saveChanges();
        });

        step("6. Open the Price tab and check the list price of the DL Unlimited", () -> {
            cartPage.openTab();

            cartPage.getQliFromCartByDisplayName(dlUnlimitedNoContract.name)
                    .getListPrice()
                    .shouldHave(exactText(steps.quoteWizard.currencyPrefix + dlUnlimitedNoContract.price));

            var dlUnlimitedQLI = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, UnitPrice " +
                            "FROM QuoteLineItem " +
                            "WHERE Quote.OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "' " +
                            "AND Product2.ExtID__c = '" + dlUnlimitedNoContract.dataName + "'",
                    QuoteLineItem.class);
            assertThat(format("%.2f", dlUnlimitedQLI.getUnitPrice()))
                    .as("QuoteLineItem.UnitPrice value for the DL Unlimited")
                    .isEqualTo(dlUnlimitedNoContract.price);
        });
    }
}