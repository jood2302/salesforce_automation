package ngbs.quotingwizard.existingbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.cartPage;
import static com.codeborne.selenide.Condition.exactText;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("NGBS")
public class RecurringChargesChangeMonthlyTest extends BaseTest {
    private final Steps steps;
    private final RecurringChargesChangeSteps recurringChargesChangeSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    public RecurringChargesChangeMonthlyTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_NonContract_RentalPhones_163080013.json",
                Dataset.class);

        steps = new Steps(data);
        recurringChargesChangeSteps = new RecurringChargesChangeSteps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    @BeforeEach
    public void setUpTest() {
        steps.ngbs.generateBillingAccount();

        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-20695")
    @DisplayName("CRM-20695 - Delta MRR field should be equal to Change in Recurring Charges from Price Tab. Monthly Charge Term")
    @Description("To check that FSC Value will be calculated correctly if user saves it from the Price or Quote Details Tabs")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select the same package with a contract, " +
                "add Rental phones, assign them to DLs, and save changes on the Price tab", () -> {
            recurringChargesChangeSteps.createNewContractedQuoteWithAssignedRentalPhones(steps.quoteWizard.opportunity.getId());
        });

        step("2. Check that Delta MRR on the Quote is equal to 'Change in Recurring Charges' from the Price Tab", () -> {
            var quote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Delta_MRR__c " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "'",
                    Quote.class);
            assertThat(quote.getDelta_MRR__c())
                    .as("Quote.Delta_MRR__c value")
                    .isNotNull();
            var deltaMrrFormatted = format("%.2f", quote.getDelta_MRR__c());

            cartPage.footer.changeInRecurringCharges.shouldHave(exactText(deltaMrrFormatted));
        });
    }
}
