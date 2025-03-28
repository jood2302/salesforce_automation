package ngbs.quotingwizard.newbusiness.signup;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.opportunityPage;
import static base.Pages.quotePage;
import static com.aquiva.autotests.rc.page.components.AreaCodeSelector.REQUIRED_AREA_CODE_ERROR;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.closeWindow;
import static com.codeborne.selenide.Selenide.switchTo;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

@Tag("P0")
@Tag("NGBS")
@Tag("SignUp")
public class SalesQuoteSignUpValidationsTest extends BaseTest {
    private final SalesQuoteSignUpSteps salesQuoteSignUpSteps;
    private final Steps steps;

    public SalesQuoteSignUpValidationsTest() {
        salesQuoteSignUpSteps = new SalesQuoteSignUpSteps();
        steps = new Steps(salesQuoteSignUpSteps.data);
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUserWithPermissionSet = salesQuoteSignUpSteps.getSalesRepUserWithAllowedProcessOrderWithoutShipping();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUserWithPermissionSet);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUserWithPermissionSet);
        salesQuoteSignUpSteps.loginAsSalesRepUserWithAllowedProcessOrderWithoutShipping();
    }

    @Test
    @TmsLink("CRM-10962")
    @TmsLink("CRM-19879")
    @DisplayName("CRM-10962 - Area Codes validation on Sign Up. \n" +
            "CRM-19879 - Main and Fax Area Code fields are required")
    @Description("CRM-10962 - Verify that the Main and Fax Area Code validations are present on the 'Process Order' button. \n" +
            "CRM-19879 - Verify that the Quote can't be saved with empty Main Area Code and Fax Area Code fields")
    public void test() {
        step("1. Open the New Business Opportunity, switch to the Quote Wizard, add a new Sales quote, " +
                "add some Products, assign devices, and save changes", () ->
                salesQuoteSignUpSteps.prepareSalesQuoteWithAssignedDevicesSteps(steps.quoteWizard.opportunity.getId())
        );

        //  CRM-19879
        step("2. Open the Quote Details tab in the Quote Wizard, " +
                "populate the 'Discount Justification' field, " +
                "and check the error message indicating that both Area Codes are missing", () -> {
            quotePage.openTab();
            quotePage.discountJustificationTextArea.setValue(TEST_STRING);

            quotePage.mainAreaCodeError
                    .shouldHave(exactTextCaseSensitive(REQUIRED_AREA_CODE_ERROR), ofSeconds(15));
            quotePage.faxAreaCodeError
                    .shouldHave(exactTextCaseSensitive(REQUIRED_AREA_CODE_ERROR), ofSeconds(15));
            quotePage.saveButton.shouldBe(disabled);
        });

        //  CRM-10962
        step("3. Press 'Process Order' on the Opportunity record page, " +
                "check that the error message is shown on the 'Preparing Data - Data validation' step in the Process Order modal window, " +
                "and close the modal window", () -> {
            opportunityPage.clickProcessOrderButton();

            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(visible, ofSeconds(60)).click();
            opportunityPage.processOrderModal.errorNotifications
                    .shouldHave(exactTexts(format(AREA_CODES_ARE_MISSING_ERROR, MVP_SERVICE)), ofSeconds(1));
            opportunityPage.processOrderModal.mvpPreparingDataActiveStep.shouldHave(exactTextCaseSensitive(DATA_VALIDATION_STEP));

            opportunityPage.processOrderModal.closeWindow();
        });

        step("4. Switch back to the Quote Details tab, " +
                "add Main Area Code, Start Date, set Payment Method = 'Credit Card', and save changes", () -> {
            switchTo().window(1);
            quotePage.setDefaultStartDate();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.selectPaymentMethod(QuotePage.CREDIT_CARD_PAYMENT_METHOD);
            quotePage.saveChanges();

            closeWindow();
        });

        step("5. Update the Quote to Active Agreement via API", () ->
                steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(steps.quoteWizard.opportunity)
        );

        //  CRM-10962
        step("6. Press 'Process Order' button on the Opportunity's record page, " +
                "verify that 'Preparing Data' step is completed, and Timezone selector is enabled for a user", () -> {
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.waitUntilMvpPreparingDataStepIsCompleted();

            opportunityPage.processOrderModal.selectTimeZonePicklist.getInput().shouldBe(enabled, ofSeconds(30));
        });
    }
}
