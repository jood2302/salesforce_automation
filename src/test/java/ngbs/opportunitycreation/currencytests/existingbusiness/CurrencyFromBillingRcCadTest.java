package ngbs.opportunitycreation.currencytests.existingbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import ngbs.opportunitycreation.currencytests.CurrencyTestSteps;
import org.junit.jupiter.api.*;

import static base.Pages.opportunityPage;
import static base.Pages.packagePage;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("NGBS")
@Tag("QOP")
public class CurrencyFromBillingRcCadTest extends BaseTest {
    private final Steps steps;
    private final CurrencyTestSteps currencySteps;

    public CurrencyFromBillingRcCadTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/opportunitycreation/currencytests/existingbusiness/RC_MVP_Monthly_NonContract_CAD_82713013.json",
                Dataset.class);

        steps = new Steps(data);
        currencySteps = new CurrencyTestSteps(data.getCurrencyIsoCode());
    }

    @BeforeEach
    public void setUpTest() {
        steps.ngbs.generateBillingAccount();

        var salesUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesUser);

        steps.sfdc.initLoginToSfdcAsTestUser(salesUser);
        steps.opportunityCreation.openQopAndPopulateRequiredFields(steps.salesFlow.account.getId());
    }

    @Test
    @TmsLink("CRM-11651")
    @DisplayName("CRM-11651 - Creation Of Opportunity with CAD Currency")
    @Description("To check that user is able to create the Opportunity from with the CAD CurrencyIsoCode on Account")
    public void test() {
        step("1. Press 'Continue to Opportunity' on the QOP, wait until New Opportunity record page is loaded, " +
                "and check Opportunity.CurrencyIsoCode value", () -> {
            steps.opportunityCreation.pressContinueToOpp();
            currencySteps.checkCurrencyIsoCodeOnOpportunity();
        });

        step("2. Switch to the Quote Wizard section, add new Sales Quote, " +
                "and save changes on the Select Package tab with the default preselected package", () -> {
            opportunityPage.switchToNGBSQW();
            steps.quoteWizard.addNewSalesQuote();
            packagePage.saveChanges();
        });

        step("3. Open the Price tab and check the Currency of Cart items",
                currencySteps::checkCurrencyOnThePriceTab
        );
    }
}
