package ngbs.quotingwizard.newbusiness.dqtab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartPage.PENDING_APPROVAL_STATUS;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.dealqualificationtab.DealQualificationPage.APPROVAL_STATUS_REVISION_PENDING;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("DQ_GOA")
@Tag("Quote_Tool_2.0")
@Tag("UQT")
public class CreateDealQualificationViaSubmitForApprovalTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final DealQualificationSteps dqSteps;

    public CreateDealQualificationViaSubmitForApprovalTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_NonContract_1TypeOfDL.json",
                Dataset.class);
        steps = new Steps(data);
        dqSteps = new DealQualificationSteps(data);
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-27158")
    @TmsLink("CRM-27159")
    @DisplayName("CRM-27158 - Deal Qualification is created when Submit for Approval button is pressed. \n" +
            "CRM-27159 - Deal Qualification status is set to Revision Pending when Revise button is pressed.")
    @Description("CRM-27158 - Verify that Deal Qualification is created when Submit for Approval Button is pressed. \n" +
            "CRM-27159 - Verify that Deal Qualification status is set to Revision Pending when Revise button is pressed.")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                        "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        step("2. Add a phone on the Add Products tab, " +
                "open the Price tab, add a > 80% Discount to DigitalLine Unlimited, and save changes", () -> {
            steps.quoteWizard.addProductsOnProductsTab(data.getNewProductsToAdd());

            cartPage.openTab();
            cartPage.setDiscountForQLItem(dqSteps.dlUnlimited.name, dqSteps.dlUnlimited.discount);
            cartPage.saveChanges();
        });

        step("3. Open the Quote Details tab, populate Main Area Code, Discount Justification, and save changes", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.discountJustificationTextArea.setValue(TEST_STRING);
            quotePage.saveChanges();
        });

        step("4. Open the Price tab, and submit the quote for approval via 'Submit for Approval' button", () -> {
            cartPage.openTab();
            cartPage.submitForApproval();
        });

        //  CRM-27158
        step("5. Check that the Deal Qualification tab is displayed and DQ Approval Status = 'Pending Approval'", () -> {
            wizardPage.dealQualificationTabButton.shouldBe(visible);
            cartPage.dqApprovalStatus.shouldHave(exactTextCaseSensitive(PENDING_APPROVAL_STATUS));
        });

        step("6. Open the Price tab, click 'Submit for Approval' button and click 'Revise' button via Revise DQ modal ", () -> {
            cartPage.submitForApprovalButton.click();
            cartPage.reviseDealQualificationModal.reviseButton.click();
        });

        //  CRM-27159
        step("7. Check that the user is redirected to the Deal Qualification tab and Approval Status = 'Revision Pending'", () -> {
            dealQualificationPage.waitUntilLoaded();
            dealQualificationPage.dqApprovalStatus.shouldHave(exactTextCaseSensitive(APPROVAL_STATUS_REVISION_PENDING));
        });
    }
}
