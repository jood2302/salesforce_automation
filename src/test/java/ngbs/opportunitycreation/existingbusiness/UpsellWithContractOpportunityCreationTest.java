package ngbs.opportunitycreation.existingbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NgbsQuoteSelectionQuoteWizardPage.NO_SALES_QUOTES_MESSAGE;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("NGBS")
@Tag("QOP")
public class UpsellWithContractOpportunityCreationTest extends BaseTest {
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;
    private final Steps steps;

    //  Test data
    private final Product digitalLineUnlimited;

    public UpsellWithContractOpportunityCreationTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/opportunitycreation/existingbusiness/RC_MVP_Monthly_Contract_QOP_163066013.json",
                Dataset.class);

        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
        steps = new Steps(data);

        digitalLineUnlimited = data.getProductByDataName("LC_DL-UNL_50");
    }

    @BeforeEach
    public void setUpTest() {
        steps.ngbs.generateBillingAccount();
        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.stepCreateContractInNGBS();
        }

        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
        steps.opportunityCreation.openQopAndPopulateRequiredFields(steps.salesFlow.account.getId());
    }

    @Test
    @TmsLink("CRM-4484")
    @DisplayName("CRM-4484 - Creating upsell opportunity (with contract)")
    @Description("To verify that upsell opportunity is created correctly from QOP")
    public void test() {
        step("1. Set new number of DLs and click 'Continue to Opportunity' button", () -> {
            opportunityCreationPage.newNumberOfDLsInput
                    .shouldBe(visible, ofSeconds(20))
                    .setValue(digitalLineUnlimited.quantity.toString());

            steps.opportunityCreation.pressContinueToOpp();
        });

        step("2. Switch to the Quote Wizard, and check that there are no created quotes there", () -> {
            opportunityPage.switchToNGBSQW();

            quoteSelectionWizardPage.salesQuotes.shouldHave(size(0));
            quoteSelectionWizardPage.noSalesQuotesNotification.shouldHave(exactTextCaseSensitive(NO_SALES_QUOTES_MESSAGE));
            quoteSelectionWizardPage.pocQuotesSection.shouldBe(hidden);

            var createdQuotes = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + opportunityPage.getCurrentRecordId() + "'",
                    Quote.class);
            assertThat(createdQuotes.size())
                    .as("Number of created quotes on the Opportunity")
                    .isEqualTo(0);
        });
    }
}
