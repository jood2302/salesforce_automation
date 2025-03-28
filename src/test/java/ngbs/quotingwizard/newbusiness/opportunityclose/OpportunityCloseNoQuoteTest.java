package ngbs.quotingwizard.newbusiness.opportunityclose;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.opportunityPage;
import static com.aquiva.autotests.rc.page.opportunity.OpportunityRecordPage.QUOTES_DO_NOT_EXIST_ERROR;
import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

@Tag("P1")
@Tag("NGBS")
@Tag("OpportunityClose")
public class OpportunityCloseNoQuoteTest extends BaseTest {
    private final Steps steps;

    public OpportunityCloseNoQuoteTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_NonContract_1TypeOfDL.json",
                Dataset.class);
        steps = new Steps(data);
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-19703")
    @DisplayName("CRM-19703 - NGBS Opportunity Close - No Quotes")
    @Description("Verify that if Opportunity don't have any Quotes then with click on Close button on Opportunity " +
            "validation don't let close the Opportunity and will be shown Error banners with reason")
    public void test() {
        step("1. Open the New Business Opportunity record page", () ->
                opportunityPage.openPage(steps.quoteWizard.opportunity.getId())
        );

        step("2. Click 'Close' button and check error notification", () -> {
            opportunityPage.clickCloseButton();

            opportunityPage.alertNotificationBlock.shouldBe(visible, ofSeconds(20));
            opportunityPage.notifications
                    .shouldHave(exactTexts(QUOTES_DO_NOT_EXIST_ERROR), ofSeconds(1));
        });
    }
}
