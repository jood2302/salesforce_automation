package ngbs.quotingwizard.newbusiness.signup;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.AreaCode;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.Calendar;

import static base.Pages.opportunityPage;
import static base.Pages.quotePage;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.setQuoteToApprovedActiveAgreement;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

@Tag("P0")
@Tag("FVT")
@Tag("SignUp")
@Tag("TaiwanMVP")
public class TaiwanSignUpErrorValidationTest extends BaseTest {
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;
    private final Steps steps;

    //  Test data
    private final String officeService;
    private final String rcUsInitialTerm;
    private final AreaCode taiwanAreaCode;

    public TaiwanSignUpErrorValidationTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Taiwan_Annual_Contract.json",
                Dataset.class);

        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
        steps = new Steps(data);

        officeService = data.packageFolders[0].name;
        rcUsInitialTerm = data.getInitialTerm();
        taiwanAreaCode = new AreaCode("Local", "Taiwan", EMPTY_STRING, "Keelung", EMPTY_STRING);
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-27788")
    @TmsLink("CRM-28003")
    @DisplayName("CRM-27788 - Validate for error toast message when Sales rep sign's-up without invoice Approval. \n" +
            "CRM-28003 - Validate appropriate error message during sign-up when there is no VAT number & no Tax Exemption Approval")
    @Description("CRM-27788 - To Validate for error toast message when Sales rep sign's-up without invoice Approval. \n" +
            "CRM-28003 - To Validate appropriate error message during sign-up when there is no VAT number & no Tax Exemption Approval")
    public void test() {
        step("1. Open the test Opportunity, switch to the Quote Wizard, add a new Sales Quote, " +
                "select a package for it, add necessary products, and save changes on the Price tab", () ->
                steps.cartTab.prepareCartTab(steps.quoteWizard.opportunity.getId())
        );

        step("2. Open the Quote Details tab, populate Area Code, select Payment Method and save changes", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(taiwanAreaCode);
            quotePage.selectPaymentMethod(QuotePage.INVOICE_PAYMENT_METHOD);
            quotePage.saveChanges();
        });

        step("3. Update the Quote to Active Agreement via API", () -> {
            var quote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "'",
                    Quote.class);

            var from = Calendar.getInstance();
            var to = Calendar.getInstance();
            to.add(Calendar.MONTH, Integer.parseInt(rcUsInitialTerm));
            quote.setStart_Date__c(from);
            quote.setInitial_Term_months__c(rcUsInitialTerm);
            quote.setEnd_Date__c(to);
            // renewal term the same as initial term, for simplicity
            quote.setTerm_months__c(rcUsInitialTerm);

            setQuoteToApprovedActiveAgreement(quote);

            enterpriseConnectionUtils.update(quote);
        });

        //  CRM-28003
        step("4. Click 'Process Order' button on the Opportunity record page, " +
                "check that the error messages are shown on the 'Preparing Data - Data validation' step " +
                "in the Process Order modal window", () -> {
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(visible, ofSeconds(60)).click();
            opportunityPage.processOrderModal.errorNotifications.shouldHave(exactTextsCaseSensitiveInAnyOrder(
                    format(OBTAIN_INVOICE_PAYMENT_APPROVAL_ERROR, MVP_SERVICE, officeService),
                    format(PROVIDE_VAT_NUMBER_ERROR, MVP_SERVICE)), ofSeconds(1));
            opportunityPage.processOrderModal.mvpPreparingDataActiveStep.shouldHave(exactTextCaseSensitive(DATA_VALIDATION_STEP));
        });
    }
}
