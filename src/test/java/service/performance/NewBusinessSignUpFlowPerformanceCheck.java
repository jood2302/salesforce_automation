package service.performance;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Opportunity;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.CREDIT_CARD_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.Constants.USER;
import static io.qameta.allure.Allure.step;

/**
 * The script used in measuring some "performance" metrics in the New Business Sign Up user flow.
 */
@PerformanceTest
public class NewBusinessSignUpFlowPerformanceCheck extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Opportunity opportunity;

    public NewBusinessSignUpFlowPerformanceCheck() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_Contract_2TypesOfDLs_RegularAndPOC.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    @BeforeEach
    public void setUpTest() {
        var runningUser = step("Obtain a current User's Id via API", () -> {
            return enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM User " +
                            "WHERE Username = '" + USER + "'",
                    User.class);
        });

        steps.salesFlow.createAccountWithContactAndContactRole(runningUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, runningUser);
        opportunity = steps.quoteWizard.opportunity;

        step("Open the Salesforce Org's login page, log in to SFDC, and open the 'Sales' app", () -> {
            loginPage.openPage().login();
            steps.sfdc.openDefaultApp();
        });
    }

    @Test
    @DisplayName("New Business Sign Up Flow check")
    @Description("Measure the 'performance' metrics of the New Business Sign Up user flow")
    public void test() {
        step("1. Open the test Opportunity, switch to the Quote Wizard, add a new Sales quote, " +
                "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunity(opportunity.getId())
        );

        step("2. Open the Quote Details tab, populate Main Area Code, Start Date, Payment Method, and save changes", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.selectPaymentMethod(CREDIT_CARD_PAYMENT_METHOD);
            quotePage.setDefaultStartDate();
            quotePage.saveChanges();
        });

        step("3. Update the current quote to the Active Agreement status via API", () -> {
            steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(opportunity);
        });

        step("4. Press 'Process Order' button on the Opportunity's record page, " +
                "verify that 'Preparing Data' step is completed, and no errors are displayed", () -> {
            openProcessOrderModalForSignUp();
        });
    }

    /**
     * Opens the Process Order modal to finish the sign-up process.
     * <br/>
     * Note: This method is created separately to be able to measure its performance via {@link PerformanceTest} logic (AOP).
     */
    public void openProcessOrderModalForSignUp() {
        opportunityPage.clickProcessOrderButton();
        opportunityPage.processOrderModal.waitUntilMvpPreparingDataStepIsCompleted();
    }
}
