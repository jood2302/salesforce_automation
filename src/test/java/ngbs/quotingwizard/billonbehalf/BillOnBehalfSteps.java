package ngbs.quotingwizard.billonbehalf;

import com.aquiva.autotests.rc.model.ngbs.dto.partner.PartnerNgbsDTO;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import ngbs.quotingwizard.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.model.ngbs.dto.account.AccountNgbsDTO.AccountPackageDTO.PackageLimitsDTO.MONTHLY_AMOUNT_LIMIT;
import static com.aquiva.autotests.rc.model.ngbs.dto.account.PaymentMethodTypeDTO.INVOICE_PAYMENT_METHOD_TYPE;
import static com.aquiva.autotests.rc.page.salesforce.account.modal.BobWholesalePartnerConfirmationModal.THIS_OPERATION_WILL_DELETE_RELATIONSHIP_WITH_PARTNER_MESSAGE;
import static com.aquiva.autotests.rc.utilities.TimeoutAssertions.assertWithTimeout;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.*;
import static com.aquiva.autotests.rc.utilities.ngbs.PartnerNgbsFactory.createBillOnBehalfPartner;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createInvoiceOnBehalfApproval;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.INVOICING_REQUEST_RECORD_TYPE;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.closeWindow;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test methods related to test cases with 'Bill-on-Behalf' payment method.
 */
public class BillOnBehalfSteps {
    private final Dataset data;
    private final QuoteWizardSteps quoteWizardSteps;
    private final CartTabSteps cartTabSteps;
    public final PartnerPaymentMethodSteps partnerPaymentMethodSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    public PartnerNgbsDTO ngbsPartner;
    public Approval__c invoiceOnBehalfApproval;
    public Approval__c invoiceApproval;

    /**
     * New instance of test methods related to test cases with 'Bill-on-Behalf' payment method.
     *
     * @param data object parsed from the JSON files with the test data
     */
    public BillOnBehalfSteps(Dataset data) {
        this.data = data;

        quoteWizardSteps = new QuoteWizardSteps(data);
        cartTabSteps = new CartTabSteps(data);
        partnerPaymentMethodSteps = new PartnerPaymentMethodSteps(data);

        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    /**
     * <p> - Create a new BoB Partner in NGBS and related Partner Account record </p>
     * <p> - Link Customer and Partner Accounts </p>
     * <p> - Create a new 'Invoice-on-behalf Request' approval and link it with Partner Account </p>
     *
     * @param customerAccount a related Customer Account
     * @param customerContact a related Customer Account'sContact
     * @param opportunity     a related Opportunity record
     * @param ownerUser       user intended to be the owner of the created records
     */
    public void setUpBillOnBehalfSteps(Account customerAccount, Contact customerContact,
                                       Opportunity opportunity, User ownerUser) {
        step("Create Bill on Behalf partner in NGBS", () -> {
            var partner = createBillOnBehalfPartner();
            ngbsPartner = createPartnerInNGBS(partner);
        });

        partnerPaymentMethodSteps.setUpPartnerAccountForCustomerAccountTestSteps((double) ngbsPartner.id,
                BILL_ON_BEHALF_PARTNER_TYPE, customerAccount, customerContact, ownerUser);

        step("Create 'Invoice-on-behalf Request' approval with 'Required' status " +
                "and link it with Partner Account using its Accounts Payable Contact via API", () -> {
            var partnerContact = getPrimaryContactOnAccount(partnerPaymentMethodSteps.partnerAccount);
            invoiceOnBehalfApproval = createInvoiceOnBehalfApproval(opportunity, customerAccount, customerContact,
                    partnerContact, ownerUser.getId());

            //  this value is necessary to approve the Invoice-on-behalf Request Approval later,
            //  and it's auto-populated only when a user creates it via Salesforce UI
            invoiceOnBehalfApproval.setSwitchInvoicePartnerAccount__c(customerAccount.getPartner_Account__c());
            enterpriseConnectionUtils.update(invoiceOnBehalfApproval);
        });
    }

    /**
     * Test steps to prepare switching Account from BoB to non-BoB customer with some additional assertions.
     *
     * @param account     an Existing Business BoB Customer Account to switch to non-BoB
     * @param opportunity an Existing Business Opportunity related to BoB Customer Account
     */
    public void prepareDataToSwitchBoBToInvoiceTestSteps(Account account, Opportunity opportunity) {
        step("Open Existing Business Bill-on-Behalf Customer Account record page and click 'Account Viewer' button", () -> {
            accountRecordPage.openPage(account.getId());
            accountRecordPage.clickAccountViewerButton();
        });

        step("On Account Viewer switch to 'Partner Operations' tab and click 'Move to Invoice' button", () -> {
            accountViewer.switchToIFrame();
            accountViewer.partnerOperationsTab.shouldBe(visible, ofSeconds(60)).click();
            accountViewer.moveToInvoiceButton.click();
        });

        step("Check text in appeared pop up window and click 'Confirm' button", () -> {
            bobWholesalePartnerConfirmationModal.modalContentMessage
                    .shouldHave(exactText(THIS_OPERATION_WILL_DELETE_RELATIONSHIP_WITH_PARTNER_MESSAGE));
            bobWholesalePartnerConfirmationModal.confirmButton.click();
            bobApprovalCreationModal.invoiceTermsPicklist.getInput().shouldBe(visible, ofSeconds(120));
        });

        step("In opened 'Invoicing Approval' creation modal check Limits, Invoice, and Payment terms values", () -> {
            //  Invoicing Terms and Sign-Up Purchase Limit match up the corresponding fields in original Invoice Request for Customer Account
            bobApprovalCreationModal.invoiceTermsPicklist.getInput()
                    .shouldHave(exactTextCaseSensitive(invoiceOnBehalfApproval.getInvoice_Terms__c()));
            bobApprovalCreationModal.paymentTermsPicklist.getInput()
                    .shouldHave(exactTextCaseSensitive(invoiceOnBehalfApproval.getPayment_Terms__c()));
            var signUpPurchaseLimit = format("%,.2f",
                    invoiceOnBehalfApproval.getSign_Up_Purchase_Limit__c());
            bobApprovalCreationModal.signUpPurchaseLimitInput
                    .shouldHave(exactValue(signUpPurchaseLimit));

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

        //  helps with saving the approval at the right time
        step("Check that some of the required fields are loaded correctly", () -> {
            bobApprovalCreationModal.opportunitySearchInput.getSelectedEntity()
                    .shouldHave(exactTextCaseSensitive(opportunity.getName()), ofSeconds(10));
            bobApprovalCreationModal.accountSearchInput.getSelectedEntity()
                    .shouldHave(exactTextCaseSensitive(account.getName()), ofSeconds(10));
        });
    }

    /**
     * Test steps to save changes in IoB Approval Creation Modal and check Status of created Invoice Approval.
     *
     * @param expectedInvoiceStatus expected Status__c of created Invoice Approval
     * @param accountId             Id of related Account with Invoice Approval
     */
    public void clickSaveButtonAndCheckInvoicingRequestTestSteps(String expectedInvoiceStatus, String accountId) {
        step("Click 'Save' button on the Invoicing Request Approval Creation Modal", () -> {
            bobApprovalCreationModal.saveButton.click();
            bobApprovalCreationModal.saveButton.shouldBe(hidden, ofSeconds(60));
        });

        step("Check that the created Invoicing Request Approval has the expected status", () -> {
            invoiceApproval = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Status__c " +
                            "FROM Approval__c " +
                            "WHERE Account__c = '" + accountId + "' " +
                            "AND RecordType.Name = '" + INVOICING_REQUEST_RECORD_TYPE + "'",
                    Approval__c.class);

            assertThat(invoiceApproval.getStatus__c())
                    .as("Invoicing Request Approval__c.Status__c value")
                    .isEqualTo(expectedInvoiceStatus);
        });
    }

    /**
     * Test steps to check that data was updated correctly in NGBS and SFDC
     * after switching from BoB to non-BoB customer.
     *
     * @param accountId Id of Account to check
     */
    public void checkUpdatedInfoOnAccountsTestSteps(String accountId) {
        step("Check Office's Account payment method in NGBS", () ->
                assertWithTimeout(() -> {
                    var officeAccountPaymentMethodType = getPaymentMethodTypeFromNGBS(data.billingId);
                    assertEquals(INVOICE_PAYMENT_METHOD_TYPE, officeAccountPaymentMethodType.currentType,
                            "Customer Account's Payment Method in NGBS");
                }, ofSeconds(20))
        );

        step("Check partner fields on the Customer Account in SFDC", () -> {
            var customerAccount = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Partner_Account__c, Partner_Account_Name__c, Parent_Partner_Account__c, Attribution_partner_id__c, " +
                            "Partner_Contact__c, Partner_ID__c, Partner_Type__c, Ultimate_Parent_Partner_ID__c, RC_Attribution_Campaign__c," +
                            "Ultimate_Partner_ID__c, Ultimate_Partner_Name__c, Payment_Method__c  " +
                            "FROM Account " +
                            "WHERE Id = '" + accountId + "'",
                    Account.class);

            assertThat(customerAccount.getPartner_Account__c())
                    .as("Customer Account.Partner_Account__c value")
                    .isNull();
            assertThat(customerAccount.getPartner_Account_Name__c())
                    .as("Customer Account.Partner_Account_Name__c value")
                    .isNull();
            assertThat(customerAccount.getParent_Partner_Account__c())
                    .as("Customer Account.Parent_Partner_Account__c value")
                    .isNull();
            assertThat(customerAccount.getPartner_Contact__c())
                    .as("Customer Account.Partner_Contact__c value")
                    .isNull();
            assertThat(customerAccount.getPartner_ID__c())
                    .as("Customer Account.Partner_ID__c value")
                    .isNull();
            assertThat(customerAccount.getPartner_Type__c())
                    .as("Customer Account.Partner_Type__c value")
                    .isNull();
            assertThat(customerAccount.getUltimate_Parent_Partner_ID__c())
                    .as("Customer Account.Ultimate_Parent_Partner_ID__c value")
                    .isNull();
            assertThat(customerAccount.getUltimate_Partner_ID__c())
                    .as("Customer Account.Ultimate_Partner_ID__c value")
                    .isNull();
            assertThat(customerAccount.getUltimate_Partner_Name__c())
                    .as("Customer Account.Ultimate_Partner_Name__c value")
                    .isNull();
            assertThat(customerAccount.getPayment_Method__c())
                    .as("Customer Account.Payment_Method__c value")
                    .isEqualTo(INVOICE_PAYMENT_METHOD);
        });
    }

    /**
     * Open the Opportunity, add a new Sales Quote via the Quote Wizard, add a phone, assign it to DLs,
     * and populate Area Code, Start Date, Renewal Term for a quote, and update it to Active Agreement via API
     *
     * @param opportunity an Opportunity to prepare for Close and Sign Up
     * @param phoneToAdd  phone licence to be added to the quote
     * @param dlUnlimited DL Unlimited license for the device assignment
     * @param renewalTerm valid renewal term value (e.g. "36")
     */
    public void prepareOpportunityToBeClosedAndSignedUp(Opportunity opportunity, Product phoneToAdd, Product dlUnlimited,
                                                        String renewalTerm) {
        step("Open the test Opportunity, switch to the Quote Wizard, add a new Sales Quote, " +
                "select a package for it, save changes, and add necessary products on the Add Products tab", () -> {
            quoteWizardSteps.prepareOpportunity(opportunity.getId());
            quoteWizardSteps.addProductsOnProductsTab(phoneToAdd);
        });

        step("Open the Price tab, and assign devices to DigitalLines", () -> {
            cartPage.openTab();
            cartTabSteps.assignDevicesToDL(phoneToAdd.name, dlUnlimited.name, quoteWizardSteps.localAreaCode,
                    phoneToAdd.quantity);
        });

        step("Open the Quote Details tab, select Main Area Code, Start Date, Renewal term and save changes", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(quoteWizardSteps.localAreaCode);
            quotePage.setDefaultStartDate();
            quotePage.renewalTermPicklist.selectOption(renewalTerm);
            quotePage.saveChanges();
            closeWindow();
        });

        step("Update the Quote to Active Agreement via API", () ->
                quoteWizardSteps.stepUpdateQuoteToApprovedActiveAgreement(opportunity)
        );
    }
}
