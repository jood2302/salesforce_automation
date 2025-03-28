package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import ngbs.quotingwizard.ProServSteps;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.OPTUS_USERS_GROUP;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

@Tag("P1")
@Tag("OpportunityClose")
@Tag("RiseBrands")
@Tag("Validations")
public class RiseInternationalOpportunityCloseValidationsTest extends BaseTest {
    private final RiseOpportunitySteps riseOpportunitySteps;
    private final ProServSteps proServSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    public RiseInternationalOpportunityCloseValidationsTest() {
        riseOpportunitySteps = new RiseOpportunitySteps(1);
        proServSteps = new ProServSteps();
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    @BeforeEach
    public void setUpTest() {
        riseOpportunitySteps.loginAndSetUpRiseOpportunitySteps(OPTUS_USERS_GROUP);
    }

    @Test
    @TmsLink("CRM-35504")
    @TmsLink("CRM-35339")
    @DisplayName("CRM-35504 - Opportunity for Rise International brand can't be closed without ProServ Quote in Status = Cancelled. \n" +
            "CRM-35339 - Contact Center Legacy Tab is hidden for Rise brands")
    @Description("CRM-35504 - Verify that Opportunity for Rise International brand can't be closed:\n" +
            " - without created ProServ Quote\n" +
            " - with ProServ Quote with ProServ Status != Cancelled. \n\n" +
            "CRM-35339 - Verify that Contact Center Legacy Tab is hidden from Opportunity page " +
            "and in UQT for Rise America and Rise International brands")
    public void test() {
        //  CRM-35339
        step("1. Open the test Opportunity record page " +
                "and check that Contact Center tab is hidden on the Quote Selection page", () -> {
            opportunityPage.openPage(riseOpportunitySteps.customerOpportunity.getId());
            opportunityPage.switchToNGBSQWIframeWithoutQuote();
            quoteSelectionWizardPage.initiateProServButton.shouldBe(visible, ofSeconds(60));

            wizardBodyPage.contactCenterTab.shouldBe(hidden);
        });

        //  CRM-35504
        step("2. Click 'Initiate ProServ' button and check that ProServ Quote is created with ProServ_Status__c = 'Created'", () -> {
            quoteSelectionWizardPage.initiateProServ();

            proServSteps.checkProServQuoteStatus(riseOpportunitySteps.customerOpportunity.getId(), CREATED_PROSERV_STATUS);
        });

        step("3. Set ProServ Quote.ProServ_Status__c = 'Cancelled' via API", () -> {
            var proServQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, ProServ_Status__c " +
                            "FROM Quote " +
                            "WHERE Opportunity.Id = '" + riseOpportunitySteps.customerOpportunity.getId() + "' " +
                            "AND RecordType.Name = '" + PROSERV_QUOTE_RECORD_TYPE + "'",
                    Quote.class);
            proServQuote.setProServ_Status__c(CANCELLED_PROSERV_STATUS);
            enterpriseConnectionUtils.update(proServQuote);
        });

        //  CRM-35504
        step("4. Switch to the Opportunity record page, click on 'Close' button, populate required fields in the Close Wizard, " +
                        "submit the form and verify that Opportunity.StageName = '7. Closed Won'",
                riseOpportunitySteps::stepCloseOpportunityAndCheckItsStatus
        );
    }
}
