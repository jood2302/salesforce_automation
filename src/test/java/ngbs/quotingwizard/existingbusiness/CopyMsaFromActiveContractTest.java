package ngbs.quotingwizard.existingbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal.AccountManagerModal;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.AccountData;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.packagePage;
import static base.Pages.quotePage;
import static com.aquiva.autotests.rc.utilities.StringHelper.INR_CURRENCY_ISO_CODE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createNewCustomerAccountInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AttachmentFactory.createAttachmentForSObject;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ContractFactory.createContract;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.OpportunityFactory.createOpportunity;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.PackageFactory.createBillingAccountPackage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.QuoteFactory.createActiveSalesAgreement;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ContractHelper.CONTRACT_STATUS_ACTIVE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.RC_EU_BRAND_NAME;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.RC_INDIA_MUMBAI_BUSINESS_IDENTITY_ID;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.enabled;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("IndiaMVP")
public class CopyMsaFromActiveContractTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Account officeAccount;
    private Account indianAccount;
    private Opportunity indianOpportunity;
    private Contract activeContract;

    //  Test data
    private final String officeServiceType;
    private final String officeInitialTerm;
    private final String activeContractFirstFileName;
    private final String activeContractSecondFileName;
    private final String draftContractAttachedFileName;
    private final Package officePackage;
    private final Package indiaPackage;

    public CopyMsaFromActiveContractTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_Contract_163077013_RC_India_NB.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        officeServiceType = data.packageFolders[0].name;
        officeInitialTerm = data.getInitialTerm();
        activeContractFirstFileName = "Master Service Agreement.pdf";
        activeContractSecondFileName = "MSA Active.pdf";
        draftContractAttachedFileName = "MSA Draft.pdf";
        officePackage = data.packageFolders[0].packages[0];
        indiaPackage = data.packageFolders[0].packages[1];
    }

    @BeforeEach
    public void setUpTest() {
        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.generateBillingAccount();
            steps.ngbs.stepCreateContractInNGBS();
        }

        var dealDeskUser = step("Find a user with 'Deal Desk Lightning' profile", () -> {
            return getUser()
                    .withProfile(DEAL_DESK_LIGHTNING_PROFILE)
                    //  to avoid issues with records sharing during the Account binding (access to the Account/Opportunity's KYC Approval)
                    .withGroupMembership(NON_GSP_GROUP)
                    .execute();
        });

        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        officeAccount = steps.salesFlow.account;

        steps.quoteWizard.createOpportunity(officeAccount, steps.salesFlow.contact, dealDeskUser);
        var officeOpportunity = steps.quoteWizard.opportunity;

        step("Create New Business Indian Account with related Contact and AccountContactRole via API", () -> {
            indianAccount = createNewCustomerAccountInSFDC(dealDeskUser,
                    new AccountData().withCurrencyIsoCode(INR_CURRENCY_ISO_CODE).withBillingCountry(INDIA_BILLING_COUNTRY));
        });

        step("Create New Business Indian Opportunity via API", () -> {
            var indianAccountContact = getPrimaryContactOnAccount(indianAccount);
            indianOpportunity = createOpportunity(indianAccount, indianAccountContact, true,
                    RC_EU_BRAND_NAME, RC_INDIA_MUMBAI_BUSINESS_IDENTITY_ID, dealDeskUser, INR_CURRENCY_ISO_CODE, officeServiceType);
        });

        step("Set Office Account's Service_Type__c and RC_Service_name__c = 'Office' via API", () -> {
            officeAccount.setService_Type__c(officeServiceType);
            officeAccount.setRC_Service_name__c(officeServiceType);
            enterpriseConnectionUtils.update(officeAccount);
        });

        step("Create a new Billing Account Package object (Package__c) for the Office Account via API", () -> {
            createBillingAccountPackage(officeAccount.getId(), officeAccount.getRC_User_ID__c(), officePackage.id,
                    data.getBrandName(), officeServiceType, CREDIT_CARD_PAYMENT_METHOD, PAID_RC_ACCOUNT_STATUS);
        });

        step("Create new Contract for Office Account, set its Status = 'Active' and attach file '"
                + activeContractFirstFileName + "' to it via API", () -> {
            activeContract = createContract(officeAccount);
            activeContract.setStatus(CONTRACT_STATUS_ACTIVE);
            createAttachmentForSObject(activeContract, activeContractFirstFileName);

            enterpriseConnectionUtils.update(activeContract);
        });

        step("Create an Active Sales Agreement for the Office Opportunity via API", () -> {
            createActiveSalesAgreement(officeOpportunity, officeInitialTerm);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);

        step("Open the Quote Wizard for the Opportunity on the Master (Office) Account to add a new Sales Quote, " +
                "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(officeOpportunity.getId())
        );
    }

    @Test
    @TmsLink("CRM-24811")
    @TmsLink("CRM-24812")
    @DisplayName("CRM-24811 - Copying MSA from active contract of parent account. \n" +
            "CRM-24812 - Copying MSA from active contract of parent account (multiple contracts)")
    @Description("CRM-24811 - Verify that MSA is copied to the Indian account from the existing business office account. \n" +
            "CRM-24812 - Verify that MSA is copied to the Indian account from the existing business office account from active Contract " +
            "(if the Account has 2 or more Contract records)")
    public void test() {
        step("1. Open the Quote Wizard for the Indian opportunity to add a new Sales Quote, " +
                "select a package for it, save changes, open the Quote Details tab " +
                "and populate Main Area Code and save changes", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(indianOpportunity.getId());
            packagePage.packageSelector.selectPackage(data.chargeTerm, officeServiceType, indiaPackage);
            packagePage.saveChanges();

            quotePage.openTab();
            quotePage.setMainAreaCode(steps.quoteWizard.indiaAreaCode);
            quotePage.saveChanges();
        });

        //  CRM-24811
        step("2. Bind Master Account with Indian Account via Account Manager modal",
                this::stepBindMasterAndIndianAccounts
        );

        step("3. Check that the attachment is copied from the parent Account's Contract to the Indian account " +
                "and has the same name", () ->
                stepCheckAttachmentName(activeContractFirstFileName)
        );

        step("4. Unbind Master Account from Indian Account via Account Manager modal",
                this::stepUnbindMasterAndIndianAccounts
        );

        step("5. Check that the new attachment is removed from Indian Account", () -> {
            var indianAccountAttachments = enterpriseConnectionUtils.query(
                    "SELECT Name " +
                            "FROM Attachment " +
                            "WHERE ParentId = '" + indianAccount.getId() + "'",
                    Attachment.class);

            assertThat(indianAccountAttachments)
                    .as("List of related 'Attachment' records on Indian Account")
                    .isEmpty();
        });

        step("6. Replace old attachment from parent Account's Contract with '" + activeContractSecondFileName + "'", () -> {
            var parentAccountContractAttachment = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM Attachment " +
                            "WHERE ParentId = '" + activeContract.getId() + "'",
                    Attachment.class);
            enterpriseConnectionUtils.delete(parentAccountContractAttachment);

            createAttachmentForSObject(activeContract, activeContractSecondFileName);
        });

        step("7. Bind Master Account with Indian Account via Account Manager modal",
                this::stepBindMasterAndIndianAccounts
        );

        step("8. Check that the new attachment is copied from the parent Account's Contract to the Indian account " +
                "and has the same name", () ->
                stepCheckAttachmentName(activeContractSecondFileName)
        );

        step("9. Unbind Master Account from Indian Account via Account Manager modal",
                this::stepUnbindMasterAndIndianAccounts
        );

        //  CRM-24812
        step("10. Create a new Contract record in 'Draft' status on Master Account " +
                "and attach file '" + draftContractAttachedFileName + "' to it via API", () -> {
            var contractInDraftStatus = createContract(officeAccount);
            createAttachmentForSObject(contractInDraftStatus, draftContractAttachedFileName);

            enterpriseConnectionUtils.update(contractInDraftStatus);
        });

        step("11. Bind Master Account with Indian Account via Account Manager modal",
                this::stepBindMasterAndIndianAccounts
        );

        step("12. Check that the new attachment is copied to Indian account from Master Account's ACTIVE Contract " +
                "and has the same name", () ->
                stepCheckAttachmentName(activeContractSecondFileName)
        );
    }

    /**
     * Bind Master and Indian Accounts via Account Binding Manager {@link AccountManagerModal} modal window.
     */
    private void stepBindMasterAndIndianAccounts() {
        step("Open Account Binding Manager modal window, select Master Account in search input and submit changes", () -> {
            quotePage.manageAccountBindingsButton.click();

            quotePage.manageAccountBindings.accountSearchInput.selectItemInCombobox(officeAccount.getName());
            quotePage.manageAccountBindings.notifications.shouldHave(size(0));
            quotePage.submitAccountBindingChanges();
        });
    }

    /**
     * Unbind Master and Indian Accounts via Account Binding Manager {@link AccountManagerModal} modal window.
     */
    private void stepUnbindMasterAndIndianAccounts() {
        step("Open Account Binding Manager modal window, remove Master Account from search input and submit changes", () -> {
            quotePage.manageAccountBindingsButton.click();

            quotePage.manageAccountBindings.removeBindingButton.shouldBe(enabled, ofSeconds(20)).click();
            quotePage.submitAccountBindingChanges();
        });
    }

    /**
     * Check that attachment from parent Account's Contract is copied to Indian Account successfully.
     *
     * @param fileName name of attached file to Master Account
     */
    private void stepCheckAttachmentName(String fileName) {
        step("Check that attachment is copied to Indian account and has the same name", () -> {
            var copiedAttachment = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Name " +
                            "FROM Attachment " +
                            "WHERE ParentId = '" + indianAccount.getId() + "'",
                    Attachment.class);

            assertThat(copiedAttachment.getName())
                    .as("Copied Attachment.Name value")
                    .isEqualTo(fileName);
        });
    }
}
