package ngbs.quotingwizard.newbusiness.opportunityclose;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.OpportunityRecordPage.QUOTE_IS_INVALID_ERROR;
import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

@Tag("P1")
@Tag("NGBS")
@Tag("OpportunityClose")
public class OpportunityCloseQuoteInvalidTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final Product dlUnlimited;
    private final Product phoneToAdd;

    public OpportunityCloseQuoteInvalidTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_NonContract_1TypeOfDL.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        phoneToAdd = data.getProductByDataName("LC_HD_959");
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-19691")
    @DisplayName("CRM-19691 - NGBS Opportunity Close - Invalid Quote")
    @Description("Verify that if Opportunity has Primary Invalid Quote then with click on Close button on " +
            "Opportunity validation don't let close the Opportunity and will be shown Error banners with reason")
    public void test() {
        step("1. Open the test Opportunity, switch to the Quote Wizard, select a package, " +
                "and add some Products on the Add Products tab", () -> {
            steps.quoteWizard.openQuoteWizardOnOpportunityRecordPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addNewSalesQuote();
            steps.quoteWizard.selectDefaultPackageFromTestData();

            steps.quoteWizard.addProductsOnProductsTab(phoneToAdd);
        });

        step("2. Open the Price tab, assign devices to the DLs, save changes, " +
                "open the Quote Details tab, populate Main Area Code, and save changes", () -> {
            cartPage.openTab();
            steps.cartTab.assignDevicesToDLAndSave(phoneToAdd.name, dlUnlimited.name, steps.quoteWizard.localAreaCode,
                    phoneToAdd.quantity);

            //  Set Main Area Code here to close opportunity later
            quotePage.openTab();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.saveChanges();
        });

        step("3. Set Quote.Quote_is_Invalid__c = true on the primary quote via API", () -> {
            var quote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "'",
                    Quote.class);

            quote.setQuote_is_Invalid__c(true);
            enterpriseConnectionUtils.update(quote);
        });

        step("4. Click 'Close' button on the Opportunity record page and check error notification", () -> {
            opportunityPage.clickCloseButton();

            opportunityPage.alertNotificationBlock.shouldBe(visible, ofSeconds(60));
            opportunityPage.notifications
                    .shouldHave(exactTexts(QUOTE_IS_INVALID_ERROR), ofSeconds(1));
        });
    }
}
