package ngbs.quotingwizard.newbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Opportunity;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.AGREEMENT_QUOTE_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.QUOTE_QUOTE_TYPE;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.refresh;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("TargetPrice")
public class AcceptRecommendationsButtonAvailabilityTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;
    private final Dataset data;

    private Opportunity opportunity;

    //  Test data
    private final String packageFolder;
    private final Package mvpPackageWithContract;
    private final Package mvpPackageNoContract;
    private final Product dlUnlimited;

    public AcceptRecommendationsButtonAvailabilityTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_Contract_1PhoneAnd1DL_RegularAndPOC.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        packageFolder = data.packageFolders[0].name;
        mvpPackageWithContract = data.packageFolders[0].packages[0];
        mvpPackageNoContract = data.packageFolders[0].packages[4];
        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        dlUnlimited.quantity = 30;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        opportunity = steps.quoteWizard.opportunity;
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-29637")
    @DisplayName("CRM-29637 - Button 'Accept Recommendations' availability")
    @Description("Verify that 'Accept Recommendations' button is disabled when Opportunity.StageName = '7. Closed Won' " +
            "or Quote is in 'Agreement' status")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new Sales Quote, " +
                "and select a package for it", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(opportunity.getId());
            packagePage.packageSelector.selectPackage(data.chargeTerm, packageFolder, mvpPackageWithContract);
        });

        step("2. Open the Price tab, set up quantity for the DL Unlimited and save changes", () -> {
            cartPage.openTab();
            steps.cartTab.setUpQuantities(dlUnlimited);
            cartPage.saveChanges();
        });

        step("3. Click on Target Price for the DL Unlimited, " +
                "click Accept Recommendations button in the Target Price Details modal window " +
                "and check that now Target Price is equal to Your Price for the DL Unlimited", () -> {
            var dlUnlimitedCartItem = cartPage.getQliFromCartByDisplayName(dlUnlimited.name);
            dlUnlimitedCartItem.getTargetPrice().click();
            cartPage.targetPriceModal.acceptRecommendationsButton.click();
            dlUnlimitedCartItem.getTargetPrice().shouldHave(exactTextCaseSensitive(dlUnlimitedCartItem.getYourPrice().getText()));
        });

        step("4. Set Opportunity.StageName = '5. Agreement', refresh the Quote Wizard, " +
                "check that 'Accept Recommendations' button is enabled in the Target Price modal window " +
                "and Target Price is equal to Your Price after clicking 'Accept Recommendations' button", () ->
                checkTargetPriceAndAcceptRecommendationsButtonAvailability(AGREEMENT_STAGE)
        );

        step("5. Set Opportunity.StageName = '6. Order', refresh the Quote Wizard, " +
                "check that 'Accept Recommendations' button is enabled in the Target Price modal window " +
                "and Target Price is equal to Your Price after clicking 'Accept Recommendations' button", () ->
                checkTargetPriceAndAcceptRecommendationsButtonAvailability(ORDER_STAGE)
        );

        step("6. Set the Quote.QuoteType__c = Agreement via API, refresh the Quote Wizard, " +
                "click on Target Price for the DL Unlimited " +
                "and check that Accept Recommendations button is disabled in the Target Price modal window", () -> {
            var quoteToUpdate = new Quote();
            quoteToUpdate.setId(wizardPage.getSelectedQuoteId());
            quoteToUpdate.setQuoteType__c(AGREEMENT_QUOTE_TYPE);
            enterpriseConnectionUtils.update(quoteToUpdate);

            refresh();
            wizardPage.waitUntilLoaded();
            cartPage.openTab();
            cartPage.getQliFromCartByDisplayName(dlUnlimited.name).getTargetPrice().click();
            cartPage.targetPriceModal.acceptRecommendationsButton.shouldBe(disabled);
        });

        step("7. Set the Quote.QuoteType__c field back to 'Quote' via API", () -> {
            var quoteToUpdate = new Quote();
            quoteToUpdate.setId(wizardPage.getSelectedQuoteId());
            quoteToUpdate.setQuoteType__c(QUOTE_QUOTE_TYPE);
            enterpriseConnectionUtils.update(quoteToUpdate);
        });

        step("8. Open the Quote Wizard for the test Opportunity to create another Sales quote, " +
                "uncheck the contract checkbox, select a package and save changes", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(opportunity.getId());
            packagePage.packageSelector.selectPackage(data.chargeTerm, packageFolder, mvpPackageNoContract);
            packagePage.saveChanges();
        });

        step("9. Open the Quote Details tab, click Make Primary button and set Opportunity.StageName = '7. Closed Won' via API", () -> {
            quotePage.openTab();
            quotePage.makePrimaryButton.click();
            wizardPage.waitUntilLoaded();

            var quoteUpdated = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, IsPrimary__c " +
                            "FROM Quote " +
                            "WHERE Id = '" + wizardPage.getSelectedQuoteId() + "'",
                    Quote.class);
            assertThat(quoteUpdated.getIsPrimary__c())
                    .as("Quote.IsPrimary__c value for the New Sales Quote")
                    .isTrue();

            opportunity.setStageName(CLOSED_WON_STAGE);
            enterpriseConnectionUtils.update(opportunity);
        });

        step("10. Refresh the Quote Wizard, open the Price tab, click on Target Price for the DL Unlimited " +
                "and check that Accept Recommendations button is disabled in the Target Price modal window", () -> {
            refresh();
            wizardPage.waitUntilLoaded();
            cartPage.openTab();
            cartPage.getQliFromCartByDisplayName(dlUnlimited.name).getTargetPrice().click();
            cartPage.targetPriceModal.acceptRecommendationsButton.shouldBe(disabled);
        });
    }

    /**
     * Check that 'Accept Recommendations' button is enabled when Opportunity.StageName = '5. Agreement' or '6. Order'
     * and Target Price is equal to Your Price for the DL Unlimited after clicking 'Accept Recommendations' button.
     *
     * @param stageName StageName field value of the Opportunity to check
     */
    private void checkTargetPriceAndAcceptRecommendationsButtonAvailability(String stageName) {
        step("Set Opportunity.StageName = '" + stageName + "' via API", () -> {
            opportunity.setStageName(stageName);
            enterpriseConnectionUtils.update(opportunity);
        });

        step("Refresh the Quote Wizard, open the Price tab, click on Target Price for the DL Unlimited " +
                "and check that Accept Recommendations button is enabled", () -> {
            refresh();
            wizardPage.waitUntilLoaded();
            cartPage.openTab();
            cartPage.getQliFromCartByDisplayName(dlUnlimited.name).getTargetPrice().click();
            cartPage.targetPriceModal.acceptRecommendationsButton.shouldBe(enabled);
        });

        step("Click Accept Recommendations button in the Target Price Details modal window " +
                "and check that now Target Price is equal to Your Price for the DL Unlimited", () -> {
            cartPage.targetPriceModal.acceptRecommendationsButton.click();

            var dlUnlimitedCartItem = cartPage.getQliFromCartByDisplayName(dlUnlimited.name);
            dlUnlimitedCartItem.getTargetPrice().shouldHave(exactTextCaseSensitive(dlUnlimitedCartItem.getYourPrice().getText()));
        });
    }
}
