package ngbs.quotingwizard.newbusiness.quotetab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.funnel.SignUpBodyFunnelDTO;
import com.aquiva.autotests.rc.model.ngbs.testdata.AreaCode;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.opportunityPage;
import static base.Pages.quotePage;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.INVOICE_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createInvoiceApprovalApproved;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.AGREEMENT_QUOTE_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("NGBS")
@Tag("PreferredLanguage")
@Tag("SignUp")
public class PreferredLanguagePicklistSignUpValidationTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User salesRepUser;

    //  Test data
    private final String preferredLanguageUpdated;
    private final String preferredLanguageUpdatedAsLocaleCode;
    private final AreaCode euAreaCode;

    public PreferredLanguagePicklistSignUpValidationTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_CH_MVP_Monthly_Contract_NoProducts.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        preferredLanguageUpdated = "German";
        preferredLanguageUpdatedAsLocaleCode = "de_DE";
        euAreaCode = new AreaCode("Local", "Germany", EMPTY_STRING, EMPTY_STRING, "69");
    }

    @BeforeEach
    public void setUpTest() {
        salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-26380")
    @TmsLink("CRM-26456")
    @DisplayName("CRM-26380 - Preferred Language picklist on the Package in the Quote Wizard for New Business. \n" +
            "CRM-26456 - Preferred Language is sent to the Funnel during SignUp")
    @Description("Verify that the Preferred Language picklist is available on the Quote Details tab if: \n" +
            "- Preferred Languages are available for chosen Package \n" +
            "- Account is a New Business \n" +
            "Preferred Language picklist cannot be changed with Quote Type = 'Agreement'. \n" +
            "CRM-26456 - Verify that the preferred language is being sent to the funnel " +
            "if selected in the Quote Wizard during quote creation")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        //  CRM-26380
        step("2. Open the Quote Details tab, check that 'Preferred Language' picklist is visible and enabled, " +
                "populate 'Preferred Language' with '" + preferredLanguageUpdated + "' value, " +
                "set Main Area Code, Payment Method, and Start Date, and save changes", () -> {
            quotePage.openTab();
            quotePage.preferredLanguagePicklist
                    .shouldBe(visible, enabled)
                    .getSelectedOption()
                    .shouldNotHave(exactTextCaseSensitive(preferredLanguageUpdated));

            quotePage.selectPreferredLanguage(preferredLanguageUpdated);
            quotePage.setDefaultStartDate();
            quotePage.setMainAreaCode(euAreaCode);
            quotePage.selectPaymentMethod(INVOICE_PAYMENT_METHOD);
            quotePage.saveChanges();
        });

        //  CRM-26380
        step("3. Set Quote's Stage = 'Agreement', and save changes, " +
                "and check that 'Preferred Language' picklist is visible and disabled, " +
                "and the '" + preferredLanguageUpdated + "' value is selected in it", () -> {
            quotePage.stagePicklist.selectOption(AGREEMENT_QUOTE_TYPE);
            quotePage.saveChanges();

            quotePage.preferredLanguagePicklist
                    .shouldBe(visible, disabled)
                    .getSelectedOption()
                    .shouldHave(exactTextCaseSensitive(preferredLanguageUpdated));
        });

        //  From here steps for CRM-26456
        step("4. Create an approved Invoicing Approval Request for the test Account via API", () -> {
            createInvoiceApprovalApproved(steps.quoteWizard.opportunity, steps.salesFlow.account, steps.salesFlow.contact,
                    salesRepUser.getId(), false);
        });

        step("5. Update the Quote to Active Agreement via API", () ->
                steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(steps.quoteWizard.opportunity)
        );

        //  to bypass Opportunity's validation on the Process Order modal
        step("6. Populate Account.VATNumber__c via API", () -> {
            steps.salesFlow.account.setVATNumber__c(TEST_STRING);
            enterpriseConnectionUtils.update(steps.salesFlow.account);
        });

        step("7. Re-login as a user with 'System Administrator' profile " +
                "and 'UI-less Sign Up Preview for Admins' Permission Set, " +
                "and open the Opportunity record page", () -> {
            var adminWithUiLessSignUpPreviewPS = getUser()
                    .withProfile(SYSTEM_ADMINISTRATOR_PROFILE)
                    .withPermissionSet(UI_LESS_SIGN_UP_PREVIEW_FOR_ADMINS_PS)
                    .execute();

            steps.sfdc.reLoginAsUserWithSessionReset(adminWithUiLessSignUpPreviewPS);

            opportunityPage.openPage(steps.quoteWizard.opportunity.getId());
        });

        step("8. Press 'Process Order' button on the Opportunity record page, " +
                "verify that 'Preparing Data' step is completed, and no errors are displayed, " +
                "select the default Timezone, and open the 'Admin Preview' tab", () -> {
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.waitUntilMvpPreparingDataStepIsCompleted();

            opportunityPage.processOrderModal.selectDefaultTimezone();

            opportunityPage.processOrderModal.adminPreviewTab.click();
            opportunityPage.processOrderModal.signUpBodyContents.shouldNotHave(exactText(EMPTY_STRING), ofSeconds(20));
        });

        //  CRM-26456
        step("9. Check that the 'Sign Up body' text contains 'preferredUserLanguage' = " + preferredLanguageUpdatedAsLocaleCode, () -> {
            var signUpBodyText = opportunityPage.processOrderModal.signUpBodyContents.getText();
            var signUpBodyObj = JsonUtils.readJson(signUpBodyText, SignUpBodyFunnelDTO.class);

            assertThat(signUpBodyObj.preferredUserLanguage)
                    .as(format("SignUpBody's 'preferredUserLanguage' value (SignUpBody contents: %s)", signUpBodyText))
                    .isEqualTo(preferredLanguageUpdatedAsLocaleCode);
        });
    }
}
