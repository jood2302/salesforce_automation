package ngbs.quotingwizard.newbusiness.opportunityclose;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Account;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.OpportunityRecordPage.EMPTY_BILLING_ADDRESS_ERROR;
import static com.aquiva.autotests.rc.page.opportunity.OpportunityRecordPage.ONLY_BILLING_COUNTRY_POPULATED_ERROR;
import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

@Tag("P1")
@Tag("NGBS")
@Tag("OpportunityClose")
public class OpportunityCloseBlankBillingAddressTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final Product dlUnlimited;
    private final Product phoneToAdd;
    private final String billingCountry;

    public OpportunityCloseBlankBillingAddressTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_NonContract_1TypeOfDL.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        phoneToAdd = data.getProductByDataName("LC_HD_959");
        billingCountry = "United States";
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-19694")
    @DisplayName("CRM-19694 - NGBS Opportunity Close - Billing Address")
    @Description("Verify that if Opportunities Account has blank Billing Address field then with click on Close button " +
            "on Opportunity validation don't let close the Opportunity and will be shown Error banners with reason")
    public void test() {
        step("1. Open the test Opportunity, switch to the Quote Wizard, select a package, " +
                "and add some Products on the Add Products tab", () -> {
            steps.quoteWizard.openQuoteWizardOnOpportunityRecordPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addNewSalesQuote();
            steps.quoteWizard.selectDefaultPackageFromTestData();

            steps.quoteWizard.addProductsOnProductsTab(phoneToAdd);
        });

        step("2. Open the Price tab, assign devices to all digital lines, save changes, " +
                "open the Quote Details tab, populate Main Area Code and save changes", () -> {
            cartPage.openTab();
            steps.cartTab.assignDevicesToDLAndSave(phoneToAdd.name, dlUnlimited.name, steps.quoteWizard.localAreaCode,
                    phoneToAdd.quantity);

            //  Set Main Area Code here to close opportunity later
            quotePage.openTab();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.saveChanges();
        });

        step("3. Clear Billing Address on Opportunity's Account via API", () -> {
            var accountToUpdate = new Account();
            accountToUpdate.setId(steps.salesFlow.account.getId());
            accountToUpdate.setFieldsToNull(new String[]{
                    "BillingCity",
                    "BillingCountry",
                    "BillingPostalCode",
                    "BillingState",
                    "BillingStreet"
            });
            enterpriseConnectionUtils.update(accountToUpdate);
        });

        step("4. Click 'Close' button on the Opportunity's record page and check error notification", () -> {
            opportunityPage.clickCloseButton();

            opportunityPage.alertNotificationBlock.shouldBe(visible, ofSeconds(20));
            opportunityPage.notifications
                    .shouldHave(exactTexts(EMPTY_BILLING_ADDRESS_ERROR), ofSeconds(1));
            opportunityPage.alertCloseButton.click();
            opportunityPage.alertNotificationBlock.shouldBe(hidden);
            opportunityPage.closeOpportunityModal.closeWindow();
        });

        step("5. Set 'Billing Country' for Opportunity's Account via API", () -> {
            var accountToUpdate = new Account();
            accountToUpdate.setId(steps.salesFlow.account.getId());
            accountToUpdate.setBillingCountry(billingCountry);
            enterpriseConnectionUtils.update(accountToUpdate);
        });

        step("6. Click 'Close' button on the Opportunity's record page and check error notification", () -> {
            opportunityPage.clickCloseButton();

            opportunityPage.notifications
                    .shouldHave(exactTexts(ONLY_BILLING_COUNTRY_POPULATED_ERROR), ofSeconds(20));
        });
    }
}
