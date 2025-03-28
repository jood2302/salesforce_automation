package ngbs.quotingwizard.existingbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
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
@Tag("NGBS")
@Tag("Contract")
public class UnitPriceChangedOnDlUnlimitedAfterApplyingContractTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final Product dlUnlimitedProductWithoutContract;
    private final Product dlUnlimitedProductWithContract;

    public UnitPriceChangedOnDlUnlimitedAfterApplyingContractTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_NonContract_RentalPhones_163080013.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        dlUnlimitedProductWithoutContract = data.getProductByDataName("LC_DL-UNL_50");
        dlUnlimitedProductWithContract = data.getProductByDataNameFromUpgradeData("LC_DL-UNL_50");
    }

    @BeforeEach
    public void setUpTest() {
        steps.ngbs.generateBillingAccount();

        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-29100")
    @DisplayName("CRM-29100 - Unit Price is changed on DigitalLine Unlimited " +
            "on the Price tab and on its QLI after applying a contract")
    @Description("Check that DL Unlimited's price is changed when applying a contract " +
            "both in the Quote Wizard and on the corresponding QuoteLineItem record")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        step("2. Switch to the Price Tab, " +
                "and check that the price on DL Unlimited there is the same as on its corresponding QuoteLineItem", () -> {
            cartPage.openTab();
            cartPage.getQliFromCartByDisplayName(dlUnlimitedProductWithoutContract.name)
                    .getListPrice()
                    .shouldHave(exactText(steps.quoteWizard.currencyPrefix + dlUnlimitedProductWithoutContract.price));

            var dlUnlimitedQLI = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, UnitPrice " +
                            "FROM QuoteLineItem " +
                            "WHERE Quote.OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "' " +
                            "AND Product2.ExtID__c = '" + dlUnlimitedProductWithoutContract.dataName + "'",
                    QuoteLineItem.class);
            var dlUnlimitedUnitPriceWithoutContract = String.valueOf(format("%.2f", dlUnlimitedQLI.getUnitPrice()));
            assertThat(dlUnlimitedUnitPriceWithoutContract)
                    .as("QuoteLineItem.UnitPrice value (price of DL Unlimited without a Contract discount)")
                    .isEqualTo(dlUnlimitedProductWithoutContract.price);
        });

        step("3. Open the Select Package Tab, check Contract checkbox, and save changes", () -> {
            packagePage.openTab();
            packagePage.packageSelector.setContractSelected(true);
            packagePage.saveChanges();
        });

        step("4. Switch to the Price Tab, " +
                "and check that the price on DL Unlimited there is the same as on its corresponding QuoteLineItem", () -> {
            cartPage.openTab();
            cartPage.getQliFromCartByDisplayName(dlUnlimitedProductWithContract.name)
                    .getListPrice()
                    .shouldHave(exactText(steps.quoteWizard.currencyPrefix + dlUnlimitedProductWithContract.price));

            var dlUnlimitedQliUpdated = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, UnitPrice " +
                            "FROM QuoteLineItem " +
                            "WHERE Quote.OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "' " +
                            "AND Product2.ExtID__c = '" + dlUnlimitedProductWithContract.dataName + "'",
                    QuoteLineItem.class);
            var dlUnlimitedUnitPriceWithContract = String.valueOf(format("%.2f", dlUnlimitedQliUpdated.getUnitPrice()));
            assertThat(dlUnlimitedUnitPriceWithContract)
                    .as("QuoteLineItem.UnitPrice value (price of DL Unlimited with a Contract discount)")
                    .isEqualTo(dlUnlimitedProductWithContract.price);
        });
    }
}
