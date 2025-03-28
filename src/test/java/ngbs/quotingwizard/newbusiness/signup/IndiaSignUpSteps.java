package ngbs.quotingwizard.newbusiness.signup;

import base.NgbsSteps;
import base.SfdcSteps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.AccountData;
import com.sforce.soap.enterprise.sobject.*;
import ngbs.SalesFlowSteps;
import ngbs.approvals.KycApprovalSteps;
import ngbs.quotingwizard.QuoteWizardSteps;
import org.openqa.selenium.WindowType;

import java.util.Arrays;

import static base.Pages.*;
import static com.aquiva.autotests.rc.model.scp.ScpOperationResponseDTO.Data.ENABLE_PHONE_RENTAL_PARAM_ID;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.MVP_SERVICE;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.YOUR_ACCOUNT_IS_BEING_PROCESSED_MESSAGE;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.INVOICE_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.StringHelper.INR_CURRENCY_ISO_CODE;
import static com.aquiva.autotests.rc.utilities.TimeoutAssertions.assertWithTimeout;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.getAccountInNGBS;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.searchAccountsByContactLastNameInNGBS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createNewCustomerAccountInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createInvoiceApprovalApproved;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ContentVersionFactory.createAttachmentForSObject;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.GroupMemberFactory.createGroupMemberForKycQueue;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.LocalSubscribedAddressFactory.createIndiaLocalSubscribedAddressRecord;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.OpportunityFactory.createOpportunity;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.PackageFactory.createBillingAccountPackage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.QuoteFactory.createActiveSalesAgreement;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.addSignedOffParticipationAgreement;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.LocalSubscribedAddressHelper.REGISTERED_ADDRESS_RECORD_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.RC_EU_BRAND_NAME;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.aquiva.autotests.rc.utilities.scp.ScpRestApiClient.getServiceParameters;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.closeWindow;
import static com.codeborne.selenide.Selenide.switchTo;
import static io.qameta.allure.Allure.step;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test methods related to India Sign Up process.
 */
public class IndiaSignUpSteps {
    private final Dataset data;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private final SfdcSteps sfdcSteps;
    private final NgbsSteps ngbsSteps;
    private final SalesFlowSteps salesFlowSteps;
    private final QuoteWizardSteps quoteWizardSteps;
    private final KycApprovalSteps kycApprovalSteps;

    public User dealDeskUserWithEditKycApprovalPermissionSet;

    private String rcIndiaAccountEnterpriseId;

    public Account rcOfficeAccount;
    public Account rcIndiaAccount;
    public Opportunity rcIndiaOpportunity;
    private Approval__c defaultKycApproval;

    //  Test data
    public final String officeService;
    private final String rcUsInitialTerm;
    private final Package rcOfficePackage;
    public final Package rcIndiaPackage;
    public final String signedOffParticipationAgreementFileName;

    /**
     * New instance for the class with the test methods/steps related to India Sign Up process.
     *
     * @param data object parsed from the JSON files with the test data
     */
    public IndiaSignUpSteps(Dataset data) {
        this.data = data;
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
        sfdcSteps = new SfdcSteps();
        ngbsSteps = new NgbsSteps(data);
        salesFlowSteps = new SalesFlowSteps(data);
        quoteWizardSteps = new QuoteWizardSteps(data);
        kycApprovalSteps = new KycApprovalSteps();

        officeService = data.packageFolders[0].name;
        rcUsInitialTerm = data.getInitialTerm();
        rcOfficePackage = data.packageFolders[0].packages[0];
        rcIndiaPackage = data.packageFolders[0].packages[1];
        signedOffParticipationAgreementFileName = "rc.png";
    }

    /**
     * Preconditions for the test cases related to signing up India accounts:
     * create the account with the contract for the Office in NGBS (if necessary),
     * create Account/Contact/Opportunity/Quote as Active Agreement for the Existing Business Office customer,
     * create Account/Contact/Opportunity/Sales Quote (LBO) for the New Business RC India customer.
     *
     * @param indiaBusinessIdentityId Business Identity ID to check the Service Plan in NGBS at the end
     *                                (e.g. "52" for RingCentral India Mumbai)
     */
    public void setUpIndiaSignUpTest(String indiaBusinessIdentityId) {
        if (ngbsSteps.isGenerateAccounts()) {
            ngbsSteps.generateBillingAccount();
            ngbsSteps.stepCreateContractInNGBS();
        }

        step("Find as a user with 'Deal Desk Lightning' profile and 'Edit KYC Approval' permission set", () -> {
            dealDeskUserWithEditKycApprovalPermissionSet = getUser()
                    .withProfile(DEAL_DESK_LIGHTNING_PROFILE)
                    .withPermissionSet(KYC_APPROVAL_EDIT_PS)
                    .execute();
            createGroupMemberForKycQueue(dealDeskUserWithEditKycApprovalPermissionSet.getId());
        });

        salesFlowSteps.createAccountWithContactAndContactRole(dealDeskUserWithEditKycApprovalPermissionSet);
        rcOfficeAccount = salesFlowSteps.account;
        quoteWizardSteps.createOpportunity(rcOfficeAccount, salesFlowSteps.contact, dealDeskUserWithEditKycApprovalPermissionSet);

        step("Set Office Account.Service_Type__c and Account.RC_Service_name__c = 'Office' via API", () -> {
            rcOfficeAccount.setService_Type__c(officeService);
            rcOfficeAccount.setRC_Service_name__c(officeService);
            enterpriseConnectionUtils.update(rcOfficeAccount);
        });

        step("Create a new Billing Account Package object (Package__c) for the Office Account via API", () -> {
            createBillingAccountPackage(rcOfficeAccount.getId(), rcOfficeAccount.getRC_User_ID__c(), rcOfficePackage.id,
                    data.getBrandName(), officeService, CREDIT_CARD_PAYMENT_METHOD, PAID_RC_ACCOUNT_STATUS);
        });

        step("Create an Active Sales Agreement for the Office Opportunity via API", () -> {
            createActiveSalesAgreement(quoteWizardSteps.opportunity, rcUsInitialTerm);
        });

        step("Create New Business RC India Account with Service_Type__c and RC_Service_name__c = 'Office' " +
                "with related Contact and AccountContactRole via API", () -> {
            rcIndiaAccount = createNewCustomerAccountInSFDC(dealDeskUserWithEditKycApprovalPermissionSet,
                    new AccountData().withCurrencyIsoCode(INR_CURRENCY_ISO_CODE).withBillingCountry(INDIA_BILLING_COUNTRY));
            rcIndiaAccount.setService_Type__c(officeService);
            rcIndiaAccount.setRC_Service_name__c(officeService);

            enterpriseConnectionUtils.update(rcIndiaAccount);
        });

        step("Create New Business Indian Opportunity via API", () -> {
            var rcIndiaContact = getPrimaryContactOnAccount(rcIndiaAccount);
            rcIndiaOpportunity = createOpportunity(rcIndiaAccount, rcIndiaContact, true,
                    RC_EU_BRAND_NAME, indiaBusinessIdentityId, dealDeskUserWithEditKycApprovalPermissionSet,
                    INR_CURRENCY_ISO_CODE, officeService);
        });

        step("Login as a user with 'Deal Desk Lightning' profile and 'Edit KYC Approval' permission set", () -> {
            sfdcSteps.initLoginToSfdcAsTestUser(dealDeskUserWithEditKycApprovalPermissionSet);
        });

        step("Open Indian Opportunity page, switch to the Quote Wizard, press 'Add New' for the Sales Quote, " +
                "select a package, and save changes", () -> {
            quoteWizardSteps.openQuoteWizardOnOpportunityRecordPage(rcIndiaOpportunity.getId());
            quoteWizardSteps.addNewSalesQuote();
            packagePage.packageSelector.selectPackage(data.chargeTerm, officeService, rcIndiaPackage);
            packagePage.saveChanges();
        });

        step("Set Quote.Enabled_LBO__c = true for the created Quote via API, and refresh the Quote Wizard tab", () -> {
            kycApprovalSteps.reopenQuoteWizardWithEnabledLBO(rcIndiaOpportunity.getId());
        });
    }

    /**
     * Test steps that check the Service plan (Billing cycle duration) values of RC India licenses
     * after signing up is completed.
     *
     * @param indiaBusinessIdentityId Business Identity ID to check the Service Plan in NGBS at the end
     *                                (e.g. "52" for RingCentral India Mumbai)
     */
    public void indiaSignUpMainTestSteps(String indiaBusinessIdentityId) {
        step("1. Open KYC Approval page, prepare KYC Approval on India account for approval and approve it via API", () -> {
            defaultKycApproval = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM Approval__c " +
                            "WHERE Account__c = '" + rcIndiaAccount.getId() + "'",
                    Approval__c.class);

            step("Attach file to 'Signed off Participation Agreement' section " +
                    "and populate 'Date of Sign-off' field via API", () -> {
                var signedOffAgreementAttachment = createAttachmentForSObject(
                        defaultKycApproval.getId(), signedOffParticipationAgreementFileName);
                addSignedOffParticipationAgreement(defaultKycApproval, signedOffAgreementAttachment);
            });

            step("Transfer the ownership of the KYC Approval " +
                    "to the user with a 'Deal Desk Lightning' profile and 'KYC_Approval_Edit' Permission Set via API", () -> {
                defaultKycApproval.setOwnerId(dealDeskUserWithEditKycApprovalPermissionSet.getId());
                enterpriseConnectionUtils.update(defaultKycApproval);
            });

            step("Open KYC Approval page, populate required fields, " +
                    "and attach required files in 'KYC Details' block", () -> {
                switchTo().newWindow(WindowType.TAB);
                kycApprovalPage.openPage(defaultKycApproval.getId());
                kycApprovalSteps.populateKycApprovalFieldsRequiredForApproval(defaultKycApproval);

                closeWindow();
                switchTo().window(1);
            });

            step("Create a new Local Subscribed Address record of 'Registered Address of Company' type " +
                    "for KYC approval via API", () -> {
                createIndiaLocalSubscribedAddressRecord(defaultKycApproval, REGISTERED_ADDRESS_RECORD_TYPE);
            });

            enterpriseConnectionUtils.approveSingleRecord(defaultKycApproval.getId());
        });

        step("2. Create Invoice Request Approval for RC India Account " +
                "with related 'Accounts Payable' AccountContactRole record, " +
                "and set Approval__c.Status = 'Approved' (all via API)", () -> {
            createInvoiceApprovalApproved(rcIndiaOpportunity, rcIndiaAccount, getPrimaryContactOnAccount(rcIndiaAccount),
                    dealDeskUserWithEditKycApprovalPermissionSet.getId(), false);
        });

        step("3. Open the Add Products tab in the Quote Wizard, add some products there, and save changes on the Price tab", () -> {
            quoteWizardSteps.addProductsOnProductsTab(rcIndiaPackage.products);
            cartPage.openTab();
            cartPage.saveChanges();
        });

        step("4. Open the Quote Details tab, link India Account with Office Account via Account Bindings Modal", () -> {
            quotePage.openTab();
            quotePage.manageAccountBindingsButton.click();
            quotePage.manageAccountBindings.accountSearchInput.selectItemInCombobox(salesFlowSteps.account.getName());
            quotePage.manageAccountBindings.notifications.shouldHave(size(0));
            quotePage.submitAccountBindingChanges();
        });

        step("5. Populate Area Code, select Payment Method = 'Invoice', check that 'Start Date' " +
                "and 'End Date' fields are populated and save changes", () -> {
            quotePage.setMainAreaCode(quoteWizardSteps.indiaAreaCode);
            quotePage.selectPaymentMethod(INVOICE_PAYMENT_METHOD);
            quotePage.startDateInput.shouldNotBe(empty);
            quotePage.endDateInput.shouldNotBe(empty);
            quotePage.saveChanges();
        });

        step("6. Update the Quote to Active Agreement via API", () -> {
            quoteWizardSteps.stepUpdateQuoteToApprovedActiveAgreement(rcIndiaOpportunity);
            closeWindow();
        });

        step("7. Press 'Process Order' button on the Opportunity's record page, " +
                "verify that 'Preparing Data' step is completed, " +
                "select the timezone, click 'Sign Up MVP', " +
                "and check that the account is processed for signing up", () -> {
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.waitUntilMvpPreparingDataStepIsCompleted();
            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(hidden);

            opportunityPage.processOrderModal.selectDefaultTimezone();
            opportunityPage.processOrderModal.signUpButton.click();

            opportunityPage.processOrderModal.signUpMvpStatus
                    .shouldHave(exactTextCaseSensitive(format(YOUR_ACCOUNT_IS_BEING_PROCESSED_MESSAGE, MVP_SERVICE)), ofSeconds(60));
        });

        step("8. Check Service plan values (Billing Cycle Duration) for Recurring and Usage licenses in NGBS", () -> {
            var accountNgbsDTO = step("Check that the account is created in NGBS via NGBS API", () -> {
                var primaryContact = getPrimaryContactOnAccount(rcIndiaAccount);

                return assertWithTimeout(() -> {
                    var accounts = searchAccountsByContactLastNameInNGBS(primaryContact.getLastName());
                    assertEquals(1, accounts.size(),
                            "Number of NGBS accounts found by the related Contact's Last Name");
                    return accounts.get(0);
                }, ofSeconds(60));
            });

            assertWithTimeout(() -> {
                var accountInfo = getAccountInNGBS(accountNgbsDTO.id);

                var actualBusinessIdentityId = accountInfo.businessIdentityId;
                var expectedBusinessIdentityId = parseInt(indiaBusinessIdentityId);
                assertEquals(expectedBusinessIdentityId, actualBusinessIdentityId,
                        "Business Identity Id of the NGBS Account with billingId = " + accountNgbsDTO.id);

                //  to be used in the next step
                rcIndiaAccountEnterpriseId = accountInfo.enterpriseAccountId;
            }, ofSeconds(60));
        });

        //  When SFDC sends 'EnablePhoneLeasing'=false to the Funnel, the Account is created without 'Enable Phone Rental' setting
        step("9. Check that the NGBS Account doesn't have a setting for 'Enable Phone Rental' via SCP API " +
                "(to check that 'EnablePhoneLeasing'=false was sent from SFDC to the Funnel)", () ->
                assertWithTimeout(() -> {
                    var serviceParametersResponse = getServiceParameters(rcIndiaAccountEnterpriseId);

                    assertNull(serviceParametersResponse.errors,
                            "Errors in the response on getting Service Parameters (should not exist)");

                    var serviceParamValues = serviceParametersResponse.data.account.userServiceParameterValues;
                    var enablePhoneRentalParamValue = Arrays.stream(serviceParamValues)
                            .filter(userServiceParameterValue -> userServiceParameterValue.parameterId.equals(ENABLE_PHONE_RENTAL_PARAM_ID))
                            .findFirst();
                    assertTrue(enablePhoneRentalParamValue.isEmpty(),
                            "User's value of Service Parameter for 'Enable Phone Rental' (should not be present)");

                    var serviceParameters = serviceParametersResponse.data.config.serviceParameters;
                    var enablePhoneRentalParameter = Arrays.stream(serviceParameters)
                            .filter(serviceParameter -> serviceParameter.parameterId.equals(ENABLE_PHONE_RENTAL_PARAM_ID))
                            .findFirst();
                    assertTrue(enablePhoneRentalParameter.isEmpty(),
                            "Service Parameter for 'Enable Phone Rental' (should not be present)");
                }, ofSeconds(20))
        );
    }
}
