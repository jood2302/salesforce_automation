package ngbs.quotingwizard.engage;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.AccountData;
import com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.INVOICE_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountContactRoleFactory.createAccountContactRole;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createNewCustomerAccountInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createInvoiceApprovalApproved;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.OpportunityFactory.createOpportunity;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.QuoteFactory.createActiveSalesAgreement;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountContactRoleHelper.BUSINESS_USER_ROLE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.getPrimaryContactOnAccount;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.CLOSED_WON_STAGE;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.closeWindow;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Tag("P1")
@Tag("Engage")
@Tag("OpportunityClose")
public class EngageCloseOppMasterSignedUpTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Account officeAccount;
    private Account engageAccount;
    private Contact engageContact;
    private Opportunity engageOpportunity;
    private User dealDeskUser;

    //  Test data
    private final String engageAccountService;
    private final String engagePackageFolderName;
    private final Package engagePackage;

    private final String officeService;
    private final String officeQuoteInitialTerm;

    public EngageCloseOppMasterSignedUpTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Annual_Contract_196116013_ED_Standalone_CC_EV_NB.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        engageAccountService = data.packageFolders[1].name;
        engagePackageFolderName = engageAccountService;
        engagePackage = data.packageFolders[1].packages[0];

        officeService = data.packageFolders[0].name;
        officeQuoteInitialTerm = data.packageFolders[0].packages[0].contractTerms.initialTerm[0];
    }

    @BeforeEach
    public void setUpTest() {
        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.generateBillingAccount();
            steps.ngbs.stepCreateContractInNGBS();
        }

        dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        officeAccount = steps.salesFlow.account;
        var officeContact = steps.salesFlow.contact;

        steps.quoteWizard.createOpportunity(officeAccount, officeContact, dealDeskUser);
        var officeOpportunity = steps.quoteWizard.opportunity;

        step("Set Office Account's Account_Payment_Method__c = 'Invoice', " +
                "Service_Type__c and RC_Service_name__c = 'Office' via API", () -> {
            officeAccount.setAccount_Payment_Method__c(AccountHelper.INVOICE_PAYMENT_METHOD);
            officeAccount.setService_Type__c(officeService);
            officeAccount.setRC_Service_name__c(officeService);
            enterpriseConnectionUtils.update(officeAccount);
        });

        step("Create Invoice Request Approval for Office Account " +
                "with related 'Accounts Payable' AccountContactRole record, " +
                "and set Approval__c.Status = 'Approved' (all via API)", () -> {
            createInvoiceApprovalApproved(officeOpportunity, officeAccount, officeContact, dealDeskUser.getId(), false);
        });

        step("Create an Active Agreement for the Office Opportunity via API", () -> {
            createActiveSalesAgreement(officeOpportunity, officeQuoteInitialTerm);
        });

        step("Create New Business Engage Account with Service_Type__c and RC_Service_name__c = '" +
                engageAccountService + "' with related Contact and AccountContactRole records via API", () -> {
            engageAccount = createNewCustomerAccountInSFDC(dealDeskUser, new AccountData(data));

            engageAccount.setService_Type__c(engageAccountService);
            engageAccount.setRC_Service_name__c(engageAccountService);
            enterpriseConnectionUtils.update(engageAccount);
            engageContact = getPrimaryContactOnAccount(engageAccount);
        });

        step("Add second contact role for Office Account with the same Contact " +
                "as on Engage Account's Primary Contact Role via API", () -> {
            createAccountContactRole(officeAccount, engageContact, BUSINESS_USER_ROLE, false);
        });

        step("Create New Business Engage Opportunity via API", () -> {
            engageOpportunity = createOpportunity(engageAccount, engageContact, true,
                    data.getBrandName(), data.businessIdentity.id, dealDeskUser, data.getCurrencyIsoCode(), engageAccountService);
        });

        step("Create Invoice Request Approval for Engage Account " +
                "with related 'Accounts Payable' AccountContactRole record, " +
                "and set Approval__c.Status = 'Approved' (all via API)", () -> {
            createInvoiceApprovalApproved(engageOpportunity, engageAccount, engageContact, dealDeskUser.getId(), false);
        });

        step("Link Office and Engage Accounts via API", () ->
                steps.engage.linkAccounts(engageAccount, officeAccount)
        );

        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    @Test
    @TmsLink("CRM-20511")
    @DisplayName("CRM-20511 - Master Account validations are passed")
    @Description("Verify that with click to Close button if all validations on Master Account are passed " +
            "then Closing Wizard is opened")
    public void test() {
        step("1. Open the Engage Opportunity, switch to the Quote Wizard, add a new Sales Quote, " +
                "select a package for it, and save changes", () -> {
            steps.quoteWizard.openQuoteWizardOnOpportunityRecordPage(engageOpportunity.getId());
            steps.quoteWizard.addNewSalesQuote();
            packagePage.packageSelector.selectPackage(data.chargeTerm, engagePackageFolderName, engagePackage);
            packagePage.saveChanges();
        });

        step("2. Open the Quote Details tab, populate Payment Method and Start Date, save changes " +
                "and update it to Active Agreement via API", () -> {
            quotePage.openTab();
            quotePage.selectPaymentMethod(INVOICE_PAYMENT_METHOD);
            quotePage.setDefaultStartDate();
            quotePage.saveChanges();

            steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(engageOpportunity);
            closeWindow();
        });

        //  'Deal Desk Lightning' user can close the Opportunity immediately 
        //  without the Close Wizard and additional validations related to Stage changing
        //  see Opportunity_Close_Setup__mdt.ProfileNames__c for the full list of profiles under validation
        step("3. Click 'Close' button on the Engage Opportunity's record page, " +
                "and check its 'StageName' and 'IsClosed' fields values in DB", () -> {
            opportunityPage.clickCloseButton();
            opportunityPage.spinner.shouldBe(visible, ofSeconds(10));
            opportunityPage.spinner.shouldBe(hidden, ofSeconds(30));
            opportunityPage.alertNotificationBlock.shouldNot(exist);

            var engageOpportunityUpdated = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, StageName, IsClosed " +
                            "FROM Opportunity " +
                            "WHERE Id = '" + engageOpportunity.getId() + "'",
                    Opportunity.class);
            assertThat(engageOpportunityUpdated.getStageName())
                    .as("Engage Opportunity.StageName value")
                    .isEqualTo(CLOSED_WON_STAGE);
            assertThat(engageOpportunityUpdated.getIsClosed())
                    .as("Engage Opportunity.IsClosed value")
                    .isTrue();
        });
    }
}
