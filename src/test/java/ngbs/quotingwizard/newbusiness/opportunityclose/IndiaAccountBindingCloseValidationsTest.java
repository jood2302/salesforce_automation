package ngbs.quotingwizard.newbusiness.opportunityclose;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.AccountData;
import com.sforce.soap.enterprise.sobject.*;
import com.sforce.ws.ConnectionException;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.OpportunityRecordPage.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.INR_CURRENCY_ISO_CODE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createNewCustomerAccountInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createInvoiceApprovalApproved;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createKycApproval;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ContentVersionFactory.createAttachmentForSObject;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.GroupMemberFactory.createGroupMemberForKycQueue;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.LocalSubscribedAddressFactory.createIndiaLocalSubscribedAddressRecord;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.OpportunityFactory.createOpportunity;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.PackageFactory.createBillingAccountPackage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.QuoteFactory.createActiveSalesAgreement;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.APPROVAL_STATUS_PENDING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.addSignedOffParticipationAgreement;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.LocalSubscribedAddressHelper.REGISTERED_ADDRESS_RECORD_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.closeWindow;
import static com.codeborne.selenide.Selenide.switchTo;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openqa.selenium.WindowType.TAB;

@Tag("P1")
@Tag("IndiaMVP")
public class IndiaAccountBindingCloseValidationsTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User dealDeskUserWithEditKycApprovalPermissionSet;
    private Account rcOfficeAccount;
    private Account indiaAccount;
    private Contact indiaAccountContact;
    private Opportunity indiaOpportunity;
    private Approval__c kycApproval;

    //  Test data
    private final String packageFolderName;
    private final Package officePackage;
    private final Package indiaPackage;
    private final String tierName;

    private final String signedOffParticipationAgreementFileName;

    private final Product existingPhone;

    public IndiaAccountBindingCloseValidationsTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_Contract_163077013_RC_India_NB.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        packageFolderName = data.packageFolders[0].name;
        officePackage = data.packageFolders[0].packages[0];
        indiaPackage = data.packageFolders[0].packages[1];
        tierName = packageFolderName;

        signedOffParticipationAgreementFileName = "rc.png";

        existingPhone = indiaPackage.products[0];
    }

    @BeforeEach
    public void setUpTest() {
        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.generateBillingAccount();
            steps.ngbs.stepCreateContractInNGBS();
        }

        step("Find as a user with 'Deal Desk Lightning' profile and 'Edit KYC Approval' permission set", () -> {
            dealDeskUserWithEditKycApprovalPermissionSet = getUser()
                    .withProfile(DEAL_DESK_LIGHTNING_PROFILE)
                    .withPermissionSet(KYC_APPROVAL_EDIT_PS)
                    .execute();
            createGroupMemberForKycQueue(dealDeskUserWithEditKycApprovalPermissionSet.getId());
        });

        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUserWithEditKycApprovalPermissionSet);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUserWithEditKycApprovalPermissionSet);

        rcOfficeAccount = steps.salesFlow.account;

        step("Populate Account.Service_Type__c and Account.RC_Service_name__c = 'Office' on Office Account via API", () -> {
            rcOfficeAccount.setService_Type__c(packageFolderName);
            rcOfficeAccount.setRC_Service_name__c(packageFolderName);
            enterpriseConnectionUtils.update(rcOfficeAccount);
        });

        step("Create a new Billing Account Package object (Package__c) for the Office Account via API", () -> {
            createBillingAccountPackage(rcOfficeAccount.getId(), rcOfficeAccount.getRC_User_ID__c(), officePackage.id,
                    data.getBrandName(), packageFolderName, CREDIT_CARD_PAYMENT_METHOD, PAID_RC_ACCOUNT_STATUS);
        });

        step("Create Active Sales Agreement for Office Opportunity via API", () -> {
            createActiveSalesAgreement(steps.quoteWizard.opportunity, data.getInitialTerm());
        });

        step("Create New Business RC India Account with related Contact and AccountContactRole records via API", () -> {
            indiaAccount = createNewCustomerAccountInSFDC(dealDeskUserWithEditKycApprovalPermissionSet,
                    new AccountData().withCurrencyIsoCode(INR_CURRENCY_ISO_CODE).withBillingCountry(INDIA_BILLING_COUNTRY));
        });

        step("Create New Business Indian Opportunity via API", () -> {
            indiaAccountContact = getPrimaryContactOnAccount(indiaAccount);
            indiaOpportunity = createOpportunity(indiaAccount, indiaAccountContact, true,
                    RC_EU_BRAND_NAME, RC_INDIA_MUMBAI_BUSINESS_IDENTITY_ID, dealDeskUserWithEditKycApprovalPermissionSet,
                    INR_CURRENCY_ISO_CODE, tierName);
        });

        step("Login as a user with 'Deal Desk Lightning' profile and 'Edit KYC Approval' permission set", () -> {
            steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUserWithEditKycApprovalPermissionSet);
        });
    }

    @Test
    @TmsLink("CRM-24543")
    @TmsLink("CRM-23881")
    @DisplayName("CRM-24543 - Closing India MVP opportunity - Account binding validations. \n" +
            "CRM-23881 - Closing India MVP opportunity - KYC Approval dependent validations")
    @Description("CRM-24543 - Verify that India MVP Opportunity can be closed only if there is linked to the master account. \n" +
            "CRM-23881 - Verify that India MVP Opportunity can be closed only if there is an approved KYC Approval Request")
    public void test() {
        step("1. Open the Quote Wizard for the India Opportunity, add a new Sales Quote, " +
                "select a package for it and save changes", () -> {
            steps.quoteWizard.openQuoteWizardDirect(indiaOpportunity.getId());
            steps.quoteWizard.addNewSalesQuote();
            packagePage.packageSelector.selectPackage(data.chargeTerm, packageFolderName, indiaPackage);
            packagePage.saveChanges();
        });

        step("2. Set Quote.Enabled_LBO__c = true for the created Quote via API, and refresh the Quote Wizard tab", () -> {
            steps.kycApproval.reopenQuoteWizardWithEnabledLBO(indiaOpportunity.getId());
        });

        step("3. Add products on the Add Products tab, open the Price tab and save changes", () -> {
            steps.quoteWizard.addProductsOnProductsTab(existingPhone);

            cartPage.openTab();
            cartPage.saveChanges();
        });

        step("4. Open the Quote Details tab, populate Main Area Code and save changes", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(steps.quoteWizard.indiaAreaCode);
            quotePage.saveChanges();
        });

        step("5. Open KYC Approval page, prepare the KYC record for approval and approve it via API", () -> {
            var kycApproval = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM Approval__c " +
                            "WHERE Account__c = '" + indiaAccount.getId() + "' ",
                    Approval__c.class);

            step("Attach file to 'Signed off Participation Agreement' section and populate 'Date of Sign-off' field via API", () -> {
                var signedOffAgreementAttachment = createAttachmentForSObject(
                        kycApproval.getId(), signedOffParticipationAgreementFileName);
                addSignedOffParticipationAgreement(kycApproval, signedOffAgreementAttachment);
            });

            step("Transfer the ownership of the KYC Approval " +
                    "to the user with a 'Deal Desk Lightning' profile and 'KYC_Approval_Edit' Permission Set via API", () -> {
                kycApproval.setOwnerId(dealDeskUserWithEditKycApprovalPermissionSet.getId());
                enterpriseConnectionUtils.update(kycApproval);
            });

            step("Open KYC Approval page, populate required fields, and attach required files in 'KYC Details' block", () -> {
                switchTo().window(0);
                kycApprovalPage.openPage(kycApproval.getId());
                steps.kycApproval.populateKycApprovalFieldsRequiredForApproval(kycApproval);
            });

            step("Create a new Local Subscribed Address record of 'Registered Address of Company' type " +
                    "for KYC Approval via API", () -> {
                createIndiaLocalSubscribedAddressRecord(kycApproval, REGISTERED_ADDRESS_RECORD_TYPE);
            });

            enterpriseConnectionUtils.approveSingleRecord(kycApproval.getId());
        });

        step("6. Create Invoice Request Approval for India Account " +
                "with related 'Accounts Payable' AccountContactRole record, " +
                "and set Approval__c.Status = 'Approved' (all via API)", () -> {
            createInvoiceApprovalApproved(indiaOpportunity, indiaAccount, indiaAccountContact,
                    dealDeskUserWithEditKycApprovalPermissionSet.getId(), false);
        });

        //  CRM-24543
        step("7. Open the India Opportunity record page, click 'Close' button on it, " +
                "and verify that the error message is shown", () -> {
            opportunityPage.openPage(indiaOpportunity.getId());
            opportunityPage.clickCloseButton();

            opportunityPage.alertNotificationBlock.shouldBe(visible, ofSeconds(20));
            opportunityPage.notifications
                    .shouldHave(exactTexts(ACTIVE_AGREEMENT_IS_REQUIRED_TO_CLOSE_ERROR, MASTER_ACCOUNT_WASNT_BOUND_ERROR),
                            ofSeconds(1));
            opportunityPage.closeErrorAlertNotifications();
            opportunityPage.alertNotificationBlock.shouldBe(hidden);
            opportunityPage.closeOpportunityModal.closeWindow();
        });

        step("8. Switch to the Quote Wizard and link India Account with Office Account via Account Bindings Modal", () -> {
            switchTo().window(1);
            quotePage.manageAccountBindingsButton.click();
            quotePage.manageAccountBindings.accountSearchInput.selectItemInCombobox(rcOfficeAccount.getName());
            quotePage.manageAccountBindings.notifications.shouldHave(size(0));
            quotePage.submitAccountBindingChanges();

            closeWindow();
            switchTo().window(0);
        });

        step("9. Set the Quote to Active Agreement via API", () ->
                steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(indiaOpportunity)
        );

        //  CRM-24543
        step("10. Click 'Close' button on the Indian Opportunity's record page, " +
                "and check its 'StageName' and 'IsClosed' fields values in DB", () -> {
            closeAndCheckOpportunity();
        });

        step("11. Re-open the Indian Opportunity to the '1. Qualify' stage via API", () -> {
            indiaOpportunity.setStageName(QUALIFY_STAGE);
            enterpriseConnectionUtils.update(indiaOpportunity);
        });

        //  CRM-23881
        step("12. Create new KYC Approval with related Signed Off Participation Agreement attachment, " +
                "and set Approval__c.Status = 'Pending Approval' via API, " +
                "re-open the Indian Opportunity record page, click 'Close' button, and verify that error message is shown", () -> {
            kycApproval = createKycApproval(indiaOpportunity, indiaAccount, signedOffParticipationAgreementFileName,
                    dealDeskUserWithEditKycApprovalPermissionSet.getId());
            kycApproval.setStatus__c(APPROVAL_STATUS_PENDING);
            enterpriseConnectionUtils.update(kycApproval);

            opportunityPage.openPage(indiaOpportunity.getId());
            opportunityPage.clickCloseButton();

            opportunityPage.alertNotificationBlock.shouldBe(visible, ofSeconds(20));
            opportunityPage.notifications.shouldHave(exactTexts(APPROVED_KYC_APPROVAL_REQUIRED_ERROR), ofSeconds(1));
            opportunityPage.closeErrorAlertNotifications();
            opportunityPage.alertNotificationBlock.shouldBe(hidden);
            opportunityPage.closeOpportunityModal.closeWindow();
        });

        step("13. Reject KYC Approval via API, click 'Close' button on the Opportunity Record page " +
                "and verify that error message is shown", () -> {
            enterpriseConnectionUtils.rejectSingleRecord(kycApproval.getId());

            opportunityPage.clickCloseButton();

            opportunityPage.alertNotificationBlock.shouldBe(visible, ofSeconds(20));
            opportunityPage.notifications.shouldHave(exactTexts(APPROVED_KYC_APPROVAL_REQUIRED_ERROR), ofSeconds(1));
            opportunityPage.closeErrorAlertNotifications();
            opportunityPage.alertNotificationBlock.shouldBe(hidden);
            opportunityPage.closeOpportunityModal.closeWindow();
        });

        step("14. Create new KYC Approval with related Signed off Participation Agreement attachment record, " +
                "open KYC Approval page, prepare KYC record for approval and approve it via API", () -> {
            var kycApprovalToBeApproved = createKycApproval(indiaOpportunity, indiaAccount,
                    signedOffParticipationAgreementFileName, dealDeskUserWithEditKycApprovalPermissionSet.getId());

            step("Open KYC Approval page, populate required fields, and attach required files in 'KYC Details' block", () -> {
                switchTo().newWindow(TAB);
                kycApprovalPage.openPage(kycApprovalToBeApproved.getId());
                steps.kycApproval.populateKycApprovalFieldsRequiredForApproval(kycApprovalToBeApproved);
            });

            step("Create a new Local Subscribed Address record of 'Registered Address of Company' type " +
                    "for KYC Approval via API", () -> {
                createIndiaLocalSubscribedAddressRecord(kycApprovalToBeApproved, REGISTERED_ADDRESS_RECORD_TYPE);
            });

            enterpriseConnectionUtils.approveSingleRecord(kycApprovalToBeApproved.getId());
            closeWindow();
        });

        //  CRM-23881
        step("15. Click 'Close' button on the Indian Opportunity's record page, " +
                "and check its 'StageName' and 'IsClosed' fields values in DB", () -> {
            closeAndCheckOpportunity();
        });
    }

    /**
     * Close the Opportunity via 'Close' button,
     * and check that its StageName and IsClosed values.
     */
    private void closeAndCheckOpportunity() throws ConnectionException {
        //  'Deal Desk Lightning' user can close the Opportunity immediately 
        //  without the Close Wizard and additional validations related to Stage changing
        //  see Opportunity_Close_Setup__mdt.ProfileNames__c for the full list of profiles under validation
        opportunityPage.clickCloseButton();
        opportunityPage.spinner.shouldBe(visible, ofSeconds(10));
        opportunityPage.spinner.shouldBe(hidden, ofSeconds(30));
        opportunityPage.alertNotificationBlock.shouldNot(exist);

        var updatedOpportunity = enterpriseConnectionUtils.querySingleRecord(
                "SELECT Id, StageName, IsClosed " +
                        "FROM Opportunity " +
                        "WHERE Id = '" + indiaOpportunity.getId() + "'",
                Opportunity.class);
        assertThat(updatedOpportunity.getStageName())
                .as("Opportunity.StageName value")
                .isEqualTo(CLOSED_WON_STAGE);
        assertThat(updatedOpportunity.getIsClosed())
                .as("Opportunity.IsClosed value")
                .isTrue();
    }
}
