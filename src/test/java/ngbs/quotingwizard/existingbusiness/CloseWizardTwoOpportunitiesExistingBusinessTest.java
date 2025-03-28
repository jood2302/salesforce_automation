package ngbs.quotingwizard.existingbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.OpportunityFactory;
import com.sforce.soap.enterprise.sobject.Opportunity;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.closewizard.CloseWizardPage.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.CLOSED_WON_STAGE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.QUALIFY_STAGE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.ACTIVE_QUOTE_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.AGREEMENT_QUOTE_TYPE;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("P1")
@Tag("OpportunityClose")
@Tag("DownsellDisposition")
public class CloseWizardTwoOpportunitiesExistingBusinessTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private String firstOpportunityId;

    //  Test data
    private final double negativeEstimated12MTotalPipelineValue;

    private final String tierName;

    public CloseWizardTwoOpportunitiesExistingBusinessTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_Contract_163073013.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        negativeEstimated12MTotalPipelineValue = -1.00;

        tierName = data.packageFolders[0].name;
    }

    @BeforeEach
    public void setUpTest() {
        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.generateBillingAccount();
            steps.ngbs.stepCreateContractInNGBS();
        }

        var dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUser);
        firstOpportunityId = steps.quoteWizard.opportunity.getId();

        step("Create a second Existing Business Opportunity via API", () -> {
            OpportunityFactory.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, false,
                    data.getBrandName(), data.businessIdentity.id, dealDeskUser, data.getCurrencyIsoCode(), tierName);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    @Test
    @TmsLink("CRM-36672")
    @TmsLink("CRM-32628")
    @DisplayName("CRM-36672 - Close Wizard flow when several Opportunities exist under one EB Account.\n" +
            "CRM-32628 - Downsell Disposition close wizard")
    @Description("CRM-36672 - To verify that when there are several Opportunities under one Existing Business Account:\n" +
            "\n" +
            " - Close Wizard appears with text 'This Opportunity will be closed " +
            "and Quotes under other not Closed or Downgraded Opportunities for this Account will be locked'\n" +
            " - Close Wizard appears and after click 'Ok' button Opportunity closed.\n" +
            "CRM-32628 - Verify that Downsell Disposition close wizard is opened if the User tries to close Opportunity with Estimated_12M_Total_Pipeline__c < 0")
    public void test() {
        step("1. Open the Quote Wizard for the first Opportunity to add a new Sales Quote, " +
                "leave the same package, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(firstOpportunityId)
        );

        step("2. Open the Quote Details tab, set Stage = 'Agreement', Start Date and save changes", () -> {
            quotePage.openTab();
            //  necessary to unlock the Start Date field
            quotePage.stagePicklist.selectOption(AGREEMENT_QUOTE_TYPE);
            quotePage.setDefaultStartDate();
            quotePage.saveChanges();
        });

        step("3. Set the Quote.Status = 'Active' via API", () -> {
            var quoteToUpdate = new Quote();
            quoteToUpdate.setId(wizardPage.getSelectedQuoteId());
            quoteToUpdate.setStatus(ACTIVE_QUOTE_STATUS);
            enterpriseConnectionUtils.update(quoteToUpdate);
        });

        //  CRM-36672
        step("4. Open the first Opportunity record page, click Close button and check that the Close Wizard is opened " +
                "and notification about locked quotes is displayed in the Close Wizard", () -> {
            opportunityPage.openPage(firstOpportunityId);
            opportunityPage.clickCloseButton();
            opportunityPage.spinner.shouldBe(visible, ofSeconds(10));
            opportunityPage.spinner.shouldBe(hidden, ofSeconds(30));
            opportunityPage.alertNotificationBlock.shouldNot(exist);
            closeWizardPage.switchToIFrame();

            closeWizardPage.closeWizardHeader.shouldHave(exactTextCaseSensitive(DO_YOU_WANT_TO_CLOSE_THIS_OPPORTUNITY));
            closeWizardPage.closeWizardText.shouldHave(exactTextCaseSensitive(THIS_OPPORTUNITY_WILL_BE_CLOSED_AND_QUOTES_WILL_BE_LOCKED));
        });

        //  CRM-36672
        step("5. Click 'Ok' button in the Close Wizard and check that the Opportunity's Stage is equal to '7. Closed Won'", () -> {
            closeWizardPage.okButton.click();
            closeWizardPage.switchFromIFrame();
            opportunityPage.waitUntilLoaded();

            var updatedFirstOpportunity = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, StageName " +
                            "FROM Opportunity " +
                            "WHERE Id = '" + firstOpportunityId + "'",
                    Opportunity.class);
            assertThat(updatedFirstOpportunity.getStageName())
                    .as("Opportunity.StageName value")
                    .isEqualTo(CLOSED_WON_STAGE);
        });

        //  CRM-32628
        step("6. Set Opportunity.Estimated_12M_Total_Pipeline__c = -1.00 via API", () -> {
            var opportunityToUpdate = new Opportunity();
            opportunityToUpdate.setId(firstOpportunityId);
            //  necessary to unlock the Estimated_12M_Total_Pipeline__c field
            opportunityToUpdate.setStageName(QUALIFY_STAGE);
            opportunityToUpdate.setEstimated_12M_Total_Pipeline__c(negativeEstimated12MTotalPipelineValue);
            enterpriseConnectionUtils.update(opportunityToUpdate);
        });

        //  CRM-32628
        step("7. Open Opportunity record page, click 'Close' button and check that the Downsell Wizard is opened " +
                "and check that 'Downsell Reason' picklist enabled, 'Downsell Sub Reason' picklist is disabled", () -> {
            opportunityPage.openPage(firstOpportunityId);
            opportunityPage.clickCloseButton();
            opportunityPage.spinner.shouldBe(visible, ofSeconds(10));
            opportunityPage.spinner.shouldBe(hidden, ofSeconds(30));
            opportunityPage.alertNotificationBlock.shouldNot(exist);

            closeWizardPage.downsellReasonPicklist.shouldBe(enabled);
            closeWizardPage.downsellSubReasonPicklist.shouldBe(disabled);
        });

        //  CRM-32628
        step("8. Select any value in 'Downsell Reason' and check that 'Downsell Sub Reason' picklist becomes enabled", () -> {
            closeWizardPage.downsellReasonPicklist.selectOption(COMPETITION_DOWNSELL_REASON);
            closeWizardPage.downsellSubReasonPicklist.shouldBe(enabled);
        });
    }
}
