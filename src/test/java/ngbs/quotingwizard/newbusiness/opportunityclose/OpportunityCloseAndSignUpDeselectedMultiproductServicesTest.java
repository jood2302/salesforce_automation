package ngbs.quotingwizard.newbusiness.opportunityclose;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.INVOICE_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.StringHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createInvoiceApprovalApproved;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.CLOSED_WON_STAGE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.setQuoteToApprovedActiveAgreement;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("P1")
@Tag("OpportunityClose")
@Tag("SignUp")
@Tag("Multiproduct-Lite")
public class OpportunityCloseAndSignUpDeselectedMultiproductServicesTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User salesRepUser;
    private Quote masterQuote;
    private List<Quote> rcCcTechnicalQuotes;

    //  Test data
    private final Map<String, Package> packageFolderNameToPackageMap;
    private final String officeService;
    private final String rcCcServiceName;
    private final String engageDigitalServiceName;

    public OpportunityCloseAndSignUpDeselectedMultiproductServicesTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_ED_EV_CC_Annual_Contract.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        packageFolderNameToPackageMap = Map.of(
                data.packageFolders[0].name, data.packageFolders[0].packages[0],
                data.packageFolders[2].name, data.packageFolders[2].packages[0],
                data.packageFolders[3].name, data.packageFolders[3].packages[0]
        );

        officeService = data.packageFolders[0].name;
        rcCcServiceName = data.packageFolders[3].name;
        engageDigitalServiceName = data.packageFolders[2].name;
    }

    @BeforeEach
    public void setUpTest() {
        salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @Tag("KnownIssue")
    @Issue("PBC-25687")
    @TmsLink("CRM-31527")
    @TmsLink("CRM-30269")
    @DisplayName("CRM-31527 - Bypass close validation for deselected Service's Opportunities. \n" +
            "CRM-30269 - Multiproduct - Sign Up with deselected packages")
    @Description("CRM-31527 - Verify that if some or all NB services are deselected on the MP Quote " +
            "then when closing the Master Opportunity those services will be bypassed through close validation. \n" +
            "CRM-30269 - Verify that Multiproduct Opportunity can be Signed Up after deselecting packages")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select MVP, RC Contact Center and Engage Digital packages for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityForMultiProduct(steps.quoteWizard.opportunity.getId(), packageFolderNameToPackageMap)
        );

        step("2. Open the Quote Details tab, select Main Area Code, Start Date, and save changes", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.setDefaultStartDate();
            quotePage.saveChanges();
        });

        //  CRM-30269
        step("3. Verify that for Master and all Technical Quotes IsQuoteHasErrors__c = true", () -> {
            masterQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, IsQuoteHasErrors__c, MasterQuote__c " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "' " +
                            "AND MasterQuote__c = '" + EMPTY_STRING + "'",
                    Quote.class);
            var allQuotes = enterpriseConnectionUtils.query(
                    "SELECT Id, IsQuoteHasErrors__c, ServiceName__c, MasterQuote__c " +
                            "FROM Quote " +
                            "WHERE " +
                            "(OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "' " +
                            "AND MasterQuote__c = '" + EMPTY_STRING + "' " +
                            ") " +
                            "OR MasterQuote__c = '" + masterQuote.getId() + "'",
                    Quote.class);

            //  TODO Known Issue PBC-25687 (Quote.IsQuoteHasErrors__c should be true, but it's false due to absence of the error about missing CC ProServ Quote)
            checkIsQuoteHasErrorsFieldsOnQuotes(allQuotes, true);

            rcCcTechnicalQuotes = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Quote " +
                            "WHERE MasterQuote__c = '" + masterQuote.getId() + "' " +
                            "AND ServiceName__c IN " + getStringListAsString(List.of(rcCcServiceName)),
                    Quote.class);
        });

        step("4. Open the Select Package tab, unselect RC CC package and save changes", () -> {
            packagePage.openTab();
            packageFolderNameToPackageMap.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals(officeService) && !entry.getKey().equals(engageDigitalServiceName))
                    .forEach(entry -> packagePage.packageSelector
                            .getPackageFolderByName(entry.getKey())
                            .expandFolder()
                            .getChildPackageByName(entry.getValue().getFullName())
                            .getUnselectButton()
                            .click());
            packagePage.saveChanges();
        });

        //  CRM-30269
        step("5. Verify that RC CC technical quote is deleted", () -> {
            var expectedDeletedQuotes = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Quote " +
                            "WHERE Id IN " + getSObjectIdsListAsString(rcCcTechnicalQuotes),
                    Quote.class);
            assertThat(expectedDeletedQuotes)
                    .as("Technical Quotes for RC Contact Center and Engage Digital Standalone services")
                    .isEmpty();
        });

        //  CRM-30269
        step("6. Verify that for Master and all Technical Quotes (Office and Engage Digital) have IsQuoteHasErrors__c = false", () -> {
            var allQuotes = enterpriseConnectionUtils.query(
                    "SELECT Id, IsQuoteHasErrors__c, ServiceName__c, MasterQuote__c " +
                            "FROM Quote " +
                            "WHERE (OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "' " +
                            "AND MasterQuote__c = '" + EMPTY_STRING + "') " +
                            "OR MasterQuote__c = '" + masterQuote.getId() + "'",
                    Quote.class);

            checkIsQuoteHasErrorsFieldsOnQuotes(allQuotes, false);
        });

        step("7. Open the Quote Details tab, check that Payment Method = 'Invoice', " +
                "uncheck Provision toggle and save changes", () -> {
            quotePage.openTab();
            quotePage.paymentMethodPicklist.getSelectedOption().shouldHave(exactTextCaseSensitive(INVOICE_PAYMENT_METHOD));
            //  to avoid Shipping group assignment validation in the Process Order modal
            quotePage.provisionToggle.click();
            quotePage.saveChanges();
        });

        step("8. Update the Master Quote to the Active Agreement stage via API", () -> {
            var masterQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "' " +
                            "AND IsMultiProductTechnicalQuote__c = false",
                    Quote.class);
            setQuoteToApprovedActiveAgreement(masterQuote);

            enterpriseConnectionUtils.update(masterQuote);
        });

        step("9. Create an Invoicing Request Approval record in Status = 'Approved' via API", () -> {
            createInvoiceApprovalApproved(steps.quoteWizard.opportunity, steps.salesFlow.account, steps.salesFlow.contact,
                    salesRepUser.getId(), true);
        });

        //  CRM-31527
        step("10. Open the Master Opportunity record page, close the Opportunity via Close Wizard, " +
                "and check that Master and Technical Opportunities stages = '7. Closed Won'", () -> {
            opportunityPage.openPage(steps.quoteWizard.opportunity.getId());
            opportunityPage.clickCloseButton();

            steps.salesFlow.waitUntilCloseWizardIsLoaded();
            closeWizardPage.submitCloseWizard();
            closeWizardPage.switchFromIFrame();
            opportunityPage.detailsTab.shouldBe(visible, ofSeconds(120));

            var masterOpportunity = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, StageName " +
                            "FROM Opportunity " +
                            "WHERE Id = '" + steps.quoteWizard.opportunity.getId() + "'",
                    Opportunity.class);
            var techOpportunities = enterpriseConnectionUtils.query(
                    "SELECT Id, StageName, Tier_Name__c " +
                            "FROM Opportunity " +
                            "WHERE MasterOpportunity__c = '" + steps.quoteWizard.opportunity.getId() + "'",
                    Opportunity.class);

            assertThat(masterOpportunity.getStageName())
                    .as("Master Opportunity.StageName value")
                    .isEqualTo(CLOSED_WON_STAGE);

            techOpportunities.forEach(techOpp ->
                    assertThat(techOpp.getStageName())
                            .as(techOpp.getTier_Name__c() + " Tech Opportunity.StageName value")
                            .isEqualTo(CLOSED_WON_STAGE));
        });

        //  CRM-30269
        step("11. Click Process Order button on Master Account's Opportunity " +
                "and check that Process Order modal window is opened and 'Sign Up' flow can be proceeded", () -> {
            opportunityPage.clickProcessOrderButton();

            opportunityPage.processOrderModal.waitUntilMvpPreparingDataStepIsCompleted();

            opportunityPage.processOrderModal.selectTimeZonePicklist.getInput().shouldBe(enabled, ofSeconds(30));
        });
    }

    /**
     * Check that IsQuoteHasErrors__c field has the expected value for all Quotes in the given list.
     *
     * @param allQuotes     list of Quotes to check
     * @param expectedValue true or false, for the Quote.IsQuoteHasErrors__c field
     */
    private void checkIsQuoteHasErrorsFieldsOnQuotes(List<Quote> allQuotes, boolean expectedValue) {
        for (var quote : allQuotes) {
            var stepDescription = quote.getMasterQuote__c() != null
                    ? "Tech Quote with ServiceName__c = " + quote.getServiceName__c()
                    : "Master Quote";
            step("Check that " + stepDescription + " has IsQuoteHasErrors__c = " + expectedValue, () ->
                    assertThat(quote.getIsQuoteHasErrors__c())
                            .as("Quote.IsQuoteHasErrors__c for " + stepDescription)
                            .isEqualTo(expectedValue)
            );
        }
    }
}
