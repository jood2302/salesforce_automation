package ngbs.quotingwizard.billonbehalf;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.AccountData;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.model.ngbs.dto.account.AccountNgbsDTO.AccountPackageDTO.PackageLimitsDTO.MONTHLY_AMOUNT_LIMIT;
import static com.aquiva.autotests.rc.model.ngbs.dto.account.PaymentMethodTypeDTO.INVOICE_ON_BEHALF_PAYMENT_METHOD_TYPE;
import static com.aquiva.autotests.rc.page.AccountViewerPage.PLEASE_SELECT_NEW_PARTNER_ERROR;
import static com.aquiva.autotests.rc.page.salesforce.account.modal.BobWholesalePartnerConfirmationModal.THIS_OPERATION_WILL_ESTABLISH_RELATIONSHIP_WITH_PARTNER_MESSAGE;
import static com.aquiva.autotests.rc.utilities.TimeoutAssertions.assertWithTimeout;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.*;
import static com.aquiva.autotests.rc.utilities.ngbs.PartnerNgbsFactory.createBillOnBehalfPartner;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountContactRoleFactory.createAccountContactRole;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createNewPartnerAccountInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createInvoiceApprovalApproved;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.PackageFactory.createBillingAccountPackage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountContactRoleHelper.ACCOUNTS_PAYABLE_ROLE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.CHANNEL_OPERATIONS_LIGHTNING_PROFILE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.getUser;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.sleep;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("P0")
@Tag("Bill-on-Behalf")
public class NonBobCustomerAccountInvoiceOnBehalfRequestTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User channelOperationsUser;
    private Account partnerAccount;
    private Approval__c invoiceRequestApproval;
    private Approval__c invoiceOnBehalfApproval;
    private Double ngbsPartnerId;

    //  Test data
    private final String serviceName;
    private final String ngbsPackageCatalogId;
    private final String newMonthlyCreditLimitValue;

    public NonBobCustomerAccountInvoiceOnBehalfRequestTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_NonContract_InvoicePM_163078013.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        steps.ngbs.isGenerateAccountsForSingleTest = true;

        serviceName = data.packageFolders[0].name;
        ngbsPackageCatalogId = data.packageFolders[0].packages[0].id;
        newMonthlyCreditLimitValue = "123456";
    }

    @BeforeEach
    public void setUpTest() {
        steps.ngbs.generateBillingAccount();

        step("Find a user with 'Channel Operations – Lightning' profile", () -> {
            channelOperationsUser = getUser().withProfile(CHANNEL_OPERATIONS_LIGHTNING_PROFILE).execute();
        });

        steps.salesFlow.createAccountWithContactAndContactRole(channelOperationsUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, channelOperationsUser);

        step("Create Bill on Behalf partner in NGBS via API", () -> {
            var ngbsPartnerObject = createBillOnBehalfPartner();
            var ngbsPartner = createPartnerInNGBS(ngbsPartnerObject);
            ngbsPartnerId = Double.valueOf(ngbsPartner.id);
        });

        step("Create Partner Account and make it 'Bill-on-Behalf' using SFDC API", () -> {
            step("Create 'Bill-on-Behalf' Partner Account via API", () -> {
                partnerAccount = createNewPartnerAccountInSFDC(channelOperationsUser, new AccountData(data));

                partnerAccount.setPartner_Type__c(BILL_ON_BEHALF_PARTNER_TYPE);
                partnerAccount.setBusinessIdentity__c(data.getBusinessIdentityName());
                partnerAccount.setPartnerStatus__c(ACTIVE_PARTNER_STATUS);
                partnerAccount.setNGBS_Partner_ID__c(ngbsPartnerId);
                enterpriseConnectionUtils.update(partnerAccount);
            });

            step("Create 'Accounts Payable' contact role and link it with Partner Account via API", () -> {
                var partnerAccountContact = getPrimaryContactOnAccount(partnerAccount);
                createAccountContactRole(partnerAccount, partnerAccountContact, ACCOUNTS_PAYABLE_ROLE, false);
            });
        });

        step("Update 'BusinessIdentity__c' and 'RC_User_ID__c' fields on the Customer Account via API", () -> {
            steps.salesFlow.account.setBusinessIdentity__c(data.getBusinessIdentityName());
            steps.salesFlow.account.setRC_User_ID__c(data.rcUserId);
            enterpriseConnectionUtils.update(steps.salesFlow.account);
        });

        step("Create 'Invoicing Request' approval with 'Approved' status for Customer Account via API", () -> {
            invoiceRequestApproval = createInvoiceApprovalApproved(steps.quoteWizard.opportunity, steps.salesFlow.account,
                    steps.salesFlow.contact, channelOperationsUser.getId(), false);
        });

        //  for automatic NGBS<->SFDC sync of Limits when Approval record is approved 
        step("Create a new Billing Account Package object (Package__c) for the Customer Account via API", () -> {
            createBillingAccountPackage(steps.salesFlow.account.getId(), data.packageId, ngbsPackageCatalogId,
                    data.getBrandName(), serviceName, INVOICE_ON_BEHALF_PAYMENT_METHOD, PAID_RC_ACCOUNT_STATUS);
        });

        step("Login as a user with 'Channel Operations – Lightning' profile", () ->
                steps.sfdc.initLoginToSfdcAsTestUser(channelOperationsUser)
        );
    }

    @Test
    @TmsLink("CRM-23932")
    @DisplayName("CRM-23932 - Invoice-on-behalf Request for non-BoB Customer Accounts")
    @Description("Verify that when a non-BoB Customer changed to a BoB Customer, Invoice-on-behalf Request is automatically generated")
    public void test() {
        step("1. Open Existing Business Customer Account record page and click 'Account Viewer' button", () -> {
            accountRecordPage.openPage(steps.salesFlow.account.getId());
            accountRecordPage.clickAccountViewerButton();
        });

        step("2. On the Account Viewer switch to 'Partner Operations' tab, click 'Switch to Partner' button, " +
                "and check the error message", () -> {
            accountViewer.switchToIFrame();
            accountViewer.partnerOperationsTab.shouldBe(visible, ofSeconds(60)).click();
            accountViewer.switchToPartnerButton.click();
            accountViewer.partnerAccountSearchInputError.shouldHave(text(PLEASE_SELECT_NEW_PARTNER_ERROR));
        });

        step("3. Select Partner Account and click 'Switch to Partner' button", () -> {
            accountViewer.partnerAccountSearchInput.selectItemInCombobox(partnerAccount.getName());
            accountViewer.switchToPartnerButton.click();
        });

        step("4. Check the text in the appeared pop-up window, and click 'Confirm' button", () -> {
            bobWholesalePartnerConfirmationModal.modalContentMessage
                    .shouldHave(exactText(THIS_OPERATION_WILL_ESTABLISH_RELATIONSHIP_WITH_PARTNER_MESSAGE));
            bobWholesalePartnerConfirmationModal.confirmButton.click();

            bobApprovalCreationModal.invoiceTermsPicklist.getInput().shouldBe(visible, ofSeconds(120));
        });

        step("5. In the opened 'New Approval: Invoice-on-behalf Request' creation modal " +
                "check the Limits, Invoice, and Payment terms values, and click 'Save' button", () -> {
            //  Invoicing Terms and Sign-Up Purchase Limit match up the corresponding fields in original Invoice Request for Customer Account
            bobApprovalCreationModal.invoiceTermsPicklist.getInput()
                    .shouldHave(exactTextCaseSensitive(invoiceRequestApproval.getInvoice_Terms__c()));
            bobApprovalCreationModal.paymentTermsPicklist.getInput()
                    .shouldHave(exactTextCaseSensitive(invoiceRequestApproval.getPayment_Terms__c()));

            var invoiceRequestApprovalLimitsUpdated = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Sign_Up_Purchase_Limit__c " +
                            "FROM Approval__c " +
                            "WHERE Id = '" + invoiceRequestApproval.getId() + "'",
                    Approval__c.class);
            var expectedSignUpPurchaseLimitValue = format("%,.2f",
                    invoiceRequestApprovalLimitsUpdated.getSign_Up_Purchase_Limit__c());
            bobApprovalCreationModal.signUpPurchaseLimitInput
                    .shouldHave(exactValue(expectedSignUpPurchaseLimitValue));

            //  Monthly Credit Limit value should be sourced from the NGBS
            var accountNGBS = getAccountInNGBS(data.billingId);
            var monthlyCreditLimit = stream(accountNGBS.getMainPackage().packageLimits)
                    .filter(packageLimit -> packageLimit.code.equals(MONTHLY_AMOUNT_LIMIT))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            format("No package limit with code = '%s' found on the NGBS account!",
                                    MONTHLY_AMOUNT_LIMIT)));

            var expectedMonthlyCreditLimitValue = format("%,.2f",
                    monthlyCreditLimit.value.doubleValue());
            bobApprovalCreationModal.monthlyCreditLimitInput
                    .shouldHave(exactValue(expectedMonthlyCreditLimitValue));
        });

        step("6. Change the 'Monthly Credit Limit' value and save changes", () -> {
            bobApprovalCreationModal.monthlyCreditLimitInput.setValue(newMonthlyCreditLimitValue);

            //  helps to avoid errors with validation of required fields on the layout
            bobApprovalCreationModal.partnerAccountsPayableContactSearchInput.getSelectedEntity().shouldBe(visible, ofSeconds(10));
            sleep(2_000);
            bobApprovalCreationModal.saveButton.click();
            bobApprovalCreationModal.saveButton.shouldBe(hidden, ofSeconds(60));
        });

        step("7. Check 'Status__c', 'Invoicing_Partner_ID__c', 'Partner_Type__c' fields " +
                "of the created 'Invoice-on-behalf Request' Approval'", () -> {
            invoiceOnBehalfApproval = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Status__c, Invoicing_Partner_ID__c, Partner_Type__c " +
                            "FROM Approval__c " +
                            "WHERE Account__c = '" + steps.salesFlow.account.getId() + "' " +
                            "AND RecordType.Name = '" + INVOICE_ON_BEHALF_REQUEST_RECORD_TYPE + "'",
                    Approval__c.class);

            assertThat(invoiceOnBehalfApproval.getStatus__c())
                    .as("Invoice-on-behalf Approval__c.Status__c value")
                    .isEqualTo(APPROVAL_STATUS_PENDING_L1);
            assertThat(invoiceOnBehalfApproval.getInvoicing_Partner_ID__c())
                    .as("Invoice-on-behalf Approval__c.Invoicing_Partner_ID__c value")
                    .isNull();
            assertThat(invoiceOnBehalfApproval.getPartner_Type__c())
                    .as("Invoice-on-behalf Approval__c.Partner_Type__c value")
                    .isNull();
        });

        step("8. Set Status__c = 'Approved' for the created Invoice-on-behalf Approval via API", () -> {
            invoiceOnBehalfApproval.setStatus__c(APPROVAL_STATUS_APPROVED);
            enterpriseConnectionUtils.update(invoiceOnBehalfApproval);
        });

        step("9. Check that the Payment method on the Customer Account is changed to 'Invoice On Behalf' in both NGBS and SFDC, " +
                "check Approved Package limits and Partner ID on the Customer Account in NGBS, " +
                "and check the Account's Partner-related fields in SFDC", () -> {
            step("Check Customer's Account payment method in NGBS", () -> {
                assertWithTimeout(() -> {
                    var officeAccountPaymentMethodType = getPaymentMethodTypeFromNGBS(data.billingId);
                    assertEquals(INVOICE_ON_BEHALF_PAYMENT_METHOD_TYPE, officeAccountPaymentMethodType.currentType,
                            "Customer Account's Payment Method in NGBS");
                }, ofSeconds(20));
            });

            step("Check Customer's Approved Package limits in NGBS", () -> {
                assertWithTimeout(() -> {
                    var accountDataInNGBS = getAccountInNGBS(data.billingId);
                    assertEquals(newMonthlyCreditLimitValue, valueOf(accountDataInNGBS.packages[0].packageLimits[0].value),
                            "Customer Account's Approved Package limits' value in NGBS");
                }, ofSeconds(20));
            });

            step("Check Customer's Account Partner ID in NGBS", () -> {
                var paymentMethodsFromNGBS = getPaymentMethodsFromNGBS(data.billingId);
                assertThat(paymentMethodsFromNGBS)
                        .as("Number of payment methods on the NGBS account")
                        .hasSizeGreaterThan(0);

                var invoiceOnBehalfPaymentMethod = paymentMethodsFromNGBS.stream()
                        .filter(paymentMethodDTO -> paymentMethodDTO.invoiceOnBehalfInfo != null)
                        .findFirst()
                        .orElseThrow(() ->
                                new AssertionError("No payment method with Invoice on Behalf info found on the NGBS account!"));

                assertThat(invoiceOnBehalfPaymentMethod.invoiceOnBehalfInfo.partnerId)
                        .as("Customer's Account Partner ID in NGBS")
                        .isNotNull();
            });

            step("Check partner fields on the Customer's Account in SFDC", () -> {
                var customerAccount = enterpriseConnectionUtils.querySingleRecord(
                        "SELECT Partner_Account__c, Partner_ID__c, Payment_Method__c " +
                                "FROM Account " +
                                "WHERE Id = '" + steps.salesFlow.account.getId() + "'",
                        Account.class);

                assertThat(customerAccount.getPartner_Account__c())
                        .as("Customer Account.Partner_Account__c value")
                        .isEqualTo(partnerAccount.getId());
                assertThat(customerAccount.getPartner_ID__c())
                        .as("Customer Account.Partner_ID__c value")
                        .isEqualTo(partnerAccount.getPartner_ID__c());
                assertThat(customerAccount.getPayment_Method__c())
                        .as("Customer Account.Payment_Method__c value")
                        .isEqualTo(INVOICE_ON_BEHALF_PAYMENT_METHOD);
            });
        });
    }
}
