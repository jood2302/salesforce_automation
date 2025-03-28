package ngbs.quotingwizard.newbusiness.opportunityclose;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.OpportunityRecordPage.*;
import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.switchTo;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

@Tag("P0")
@Tag("P1")
@Tag("NGBS")
@Tag("OpportunityClose")
public class OpportunityCloseInitialTermActiveAgreementTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;

    //  Test data
    private final Product dlUnlimited;
    private final String packageFolderName;
    private final Package packageToSelect;
    private final Product rentalPhone;
    private final String initialTermSufficient;
    private final String initialTermInsufficient;

    public OpportunityCloseInitialTermActiveAgreementTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_NonContract_1TypeOfDL.json",
                Dataset.class);
        steps = new Steps(data);

        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        packageFolderName = data.packageFolders[0].name;
        packageToSelect = data.packageFolders[0].packages[2];

        rentalPhone = data.getProductByDataName("LC_HDR_619", packageToSelect);

        var initialTerms = packageToSelect.contractTerms.initialTerm;
        initialTermSufficient = initialTerms[1];
        initialTermInsufficient = initialTerms[0];
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-12764")
    @TmsLink("CRM-19709")
    @DisplayName("CRM-12764 - NGBS Opp - Validation on Close for Rental Phones in Cart. \n" +
            "CRM-19709 - NGBS Opportunity Close - Active Sales Agreement.")
    @Description("CRM-12764 - Verify that on NGBS Opportunity can't be closed with contract with Initial Term of less than 24 months. \n" +
            "CRM-19709 - Verify that if NGBS Opportunity should have Active Sales Agreement.")
    public void test() {
        step("1. Open the test Opportunity, switch to the Quote Wizard, " +
                "add a new Sales quote, and select a package with contract for it", () -> {
            steps.quoteWizard.openQuoteWizardOnOpportunityRecordPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addNewSalesQuote();
            packagePage.packageSelector.selectPackage(data.chargeTerm, packageFolderName, packageToSelect);
        });

        step("2. Add a phone on the Add Products tab, " +
                "open the Price tab, assign rental phone to DigitalLine, and save changes", () -> {
            steps.quoteWizard.addProductsOnProductsTab(rentalPhone);

            cartPage.openTab();
            steps.cartTab.assignDevicesToDLAndSave(rentalPhone.name, dlUnlimited.name, steps.quoteWizard.localAreaCode,
                    rentalPhone.quantity);
        });

        step("3. Open the Quote Details tab, set Main Area Code, 'Initial Term' = 24 months or higher, and save changes", () -> {
            quotePage.openTab();

            //  Set Main Area Code here to close opportunity later
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);

            quotePage.initialTermPicklist.selectOptionContainingText(initialTermSufficient);
            quotePage.saveChanges();
        });

        //  CRM-19709
        step("4. Click 'Close' button on the Opportunity record page, and check error notification", () -> {
            opportunityPage.clickCloseButton();

            opportunityPage.alertNotificationBlock.shouldBe(visible, ofSeconds(30));
            opportunityPage.notifications
                    .shouldHave(exactTexts(ACTIVE_AGREEMENT_IS_REQUIRED_TO_CLOSE_ERROR), ofSeconds(1));
            opportunityPage.alertCloseButton.click();
            opportunityPage.alertNotificationBlock.shouldBe(hidden);
            opportunityPage.closeOpportunityModal.closeWindow();
        });

        step("5. Switch back to the quote in the Quote Wizard, " +
                "set the Start Date, 'Initial Term' = 23 months or less, and save changes", () -> {
            switchTo().window(1);
            quotePage.setDefaultStartDate();
            quotePage.initialTermPicklist.selectOptionContainingText(initialTermInsufficient);
            quotePage.saveChanges();
        });

        step("6. Set the quote to Active Agreement via API", () ->
                steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(steps.quoteWizard.opportunity)
        );

        //  CRM-12764
        step("7. Click 'Close' button on the Opportunity record page and check the error notifications", () -> {
            opportunityPage.clickCloseButton();

            opportunityPage.alertNotificationBlock.shouldBe(visible, ofSeconds(30));
            opportunityPage.notifications.shouldHave(exactTextsCaseSensitiveInAnyOrder(
                    ERRORS_ON_QUOTE_ERROR, RENTAL_PHONES_REQUIRE_CONTRACT_ERROR), ofSeconds(1));
        });
    }
}
