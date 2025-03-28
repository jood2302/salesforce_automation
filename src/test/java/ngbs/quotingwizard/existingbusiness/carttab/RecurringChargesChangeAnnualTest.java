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

import java.math.BigDecimal;

import static base.Pages.cartPage;
import static com.codeborne.selenide.Condition.exactText;
import static io.qameta.allure.Allure.step;
import static java.math.RoundingMode.HALF_DOWN;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("NGBS")
public class RecurringChargesChangeAnnualTest extends BaseTest {
    private final Steps steps;
    private final RecurringChargesChangeSteps recurringChargesChangeSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final int scale;

    public RecurringChargesChangeAnnualTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Annual_NonContract_RentalPhones_163072013.json",
                Dataset.class);

        steps = new Steps(data);
        recurringChargesChangeSteps = new RecurringChargesChangeSteps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        scale = 2;
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
    @TmsLink("CRM-22286")
    @DisplayName("CRM-22286 - Delta ARR field should be equal to Change in Recurring Charges from the Price Tab. Annual Charge Term.")
    @Description("To check that FSC Value will be calculated correctly if user saves it from the Price or Quote Details Tabs")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select the same package with a contract, add Rental phones, assign them to DLs, " +
                "and save changes on the Price tab", () ->
                recurringChargesChangeSteps.createNewContractedQuoteWithAssignedRentalPhones(steps.quoteWizard.opportunity.getId())
        );

        step("2. Check that Delta ARR on the Quote is equal to 'Change in Recurring Charges' from the Price Tab", () -> {
            var quote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Delta_ARR__c " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "'",
                    Quote.class);
            assertThat(quote.getDelta_ARR__c())
                    .as("Quote.Delta_ARR__c value")
                    .isNotNull();

            var expectedDeltaARR = BigDecimal.valueOf(quote.getDelta_ARR__c()).setScale(scale, HALF_DOWN).toString();

            cartPage.footer.changeInRecurringCharges.shouldHave(exactText(expectedDeltaARR));
        });
    }
}
