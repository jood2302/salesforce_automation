package ngbs.approvals;

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

import static base.Pages.approvalPage;
import static base.Pages.opportunityPage;
import static com.aquiva.autotests.rc.page.opportunity.modal.InvoiceOnBehalfApprovalCreationModal.INCORRECT_ACCOUNTS_PAYABLE_CONTACT_ERROR;
import static com.aquiva.autotests.rc.page.opportunity.modal.InvoiceOnBehalfApprovalCreationModal.INCORRECT_PARTNER_ACCOUNTS_PAYABLE_CONTACT_ERROR;
import static com.aquiva.autotests.rc.utilities.StringHelper.getRandomPositiveInteger;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountContactRoleFactory.createAccountContactRole;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createNewPartnerAccountInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ContactFactory.createContactForAccount;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountContactRoleHelper.ACCOUNTS_PAYABLE_ROLE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountContactRoleHelper.DECISION_MAKER_ROLE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ContactHelper.getFullName;
import static com.codeborne.selenide.CollectionCondition.itemWithText;
import static com.codeborne.selenide.Condition.exactValue;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("P1")
@Tag("Approval")
public class InvoiceOnBehalfApprovalTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Account customerAccount;
    private Account partnerAccount;
    private String partnerAccountContactFullName;
    private Contact secondCustomerAccountContact;
    private Contact secondPartnerAccountContact;

    public InvoiceOnBehalfApprovalTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/Avaya_Office_Monthly_Contract_1TypeOfDLs.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    @BeforeEach
    public void setUpTest() {
        var salesUser = steps.salesFlow.getSalesRepUser();

        steps.salesFlow.createAccountWithContactAndContactRole(salesUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesUser);
        customerAccount = steps.salesFlow.account;

        step("Create 'Bill-on-Behalf' Partner Account with related Contact and Primary Signatory AccountContactRole via API", () -> {
            partnerAccount = createNewPartnerAccountInSFDC(salesUser, new AccountData(data));
            partnerAccountContactFullName = getFullName(getPrimaryContactOnAccount(partnerAccount));
        });

        step("Update Partner_Type__c, BusinessIdentity__c, PartnerStatus__c, NGBS_Partner_ID__c fields " +
                "on the Partner Account via API", () -> {
            partnerAccount.setPartner_Type__c(BILL_ON_BEHALF_PARTNER_TYPE);
            partnerAccount.setBusinessIdentity__c(data.getBusinessIdentityName());
            partnerAccount.setPartnerStatus__c(ACTIVE_PARTNER_STATUS);
            var ngbsPartnerId = Double.valueOf(getRandomPositiveInteger());
            partnerAccount.setNGBS_Partner_ID__c(ngbsPartnerId);

            enterpriseConnectionUtils.update(partnerAccount);
        });

        step("Update Primary 'Signatory' Partner Contact's role to 'Accounts Payable', " +
                "create another contact with 'Decision Maker' contact role for partner Account via API", () -> {
            var primarySignatoryRoleOnPartnerAccount = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM AccountContactRole " +
                            "WHERE AccountId = '" + partnerAccount.getId() + "' " +
                            "AND isPrimary = true",
                    AccountContactRole.class);
            primarySignatoryRoleOnPartnerAccount.setRole(ACCOUNTS_PAYABLE_ROLE);
            enterpriseConnectionUtils.update(primarySignatoryRoleOnPartnerAccount);

            secondPartnerAccountContact = createContactForAccount(partnerAccount, salesUser);
            createAccountContactRole(partnerAccount, secondPartnerAccountContact, DECISION_MAKER_ROLE, false);
        });

        step("Link Customer Account with Partner Account " +
                "using Partner_Type__c, Partner_Account__c fields via API", () -> {
            customerAccount.setPartner_Type__c(BILL_ON_BEHALF_PARTNER_TYPE);
            customerAccount.setBusinessIdentity__c(data.getBusinessIdentityName());
            customerAccount.setPartner_ID__c(partnerAccount.getId());
            customerAccount.setPartner_Account__c(partnerAccount.getId());
            enterpriseConnectionUtils.update(customerAccount);
        });

        step("Update Primary 'Signatory' contact role to 'Accounts Payable', " +
                "create another contact with 'Decision Maker' contact role for customer Account via API", () -> {
            var primarySignatoryRoleOnCustomerAccount = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM AccountContactRole " +
                            "WHERE AccountId = '" + customerAccount.getId() + "' " +
                            "AND isPrimary = true",
                    AccountContactRole.class);
            primarySignatoryRoleOnCustomerAccount.setRole(ACCOUNTS_PAYABLE_ROLE);
            enterpriseConnectionUtils.update(primarySignatoryRoleOnCustomerAccount);

            secondCustomerAccountContact = createContactForAccount(customerAccount, salesUser);
            createAccountContactRole(customerAccount, secondCustomerAccountContact, DECISION_MAKER_ROLE, false);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesUser);
    }

    @Test
    @TmsLink("CRM-23382")
    @TmsLink("CRM-23458")
    @DisplayName("CRM-23382 - New Approval 'Invoice-on-behalf' can be created and auto populates with Account/Opportunity information. \n" +
            "CRM-23458 - Only Account's Payable Contact can be selected on Invoicing-on-behalf Request Layout")
    @Description("CRM-23382 - Verify that new Approval record type Invoice-on-behalf is created. \n" +
            "Sales rep can create 'Invoice-on-behalf' request and when He creates it, " +
            "'Invoice-on-behalf' request auto populates with: \n" +
            "Account(Partner's and Customer's contacts with account payable contact role) information\n" +
            "Opportunity Information. \n" +
            "CRM-23458 - Verify that Accounts Payable Contact AND Partner's Accounts Payable Contact fields " +
            "on Invoicing-on-behalf Request Form can be populated only with Contacts with Accounts Payable role")
    public void test() {
        step("1. Open New Business Opportunity and proceed to Invoice-on-behalf Request approval creation modal", () -> {
            opportunityPage.openPage(steps.quoteWizard.opportunity.getId());
            opportunityPage.waitUntilLoaded();

            opportunityPage.openCreateNewApprovalModal();
        });

        step("2. Select 'Invoice-on-behalf Request' record type and proceed to Approval creation form", () -> {
            opportunityPage.newApprovalRecordTypeSelectionModal.selectApprovalType(INVOICE_ON_BEHALF_REQUEST_RECORD_TYPE);
            opportunityPage.newApprovalRecordTypeSelectionModal.nextButton.click();
        });

        //  CRM-23382
        step("3. Verify that Opportunity, Account and Accounts Payable Contacts fields are populated automatically", () -> {
            opportunityPage.invoiceOnBehalfApprovalCreationModal.opportunitySearchLookup.getSelf().shouldBe(visible, ofSeconds(10));
            opportunityPage.invoiceOnBehalfApprovalCreationModal.opportunitySearchLookup.getInput()
                    .shouldHave(exactValue(steps.quoteWizard.opportunity.getName()));
            opportunityPage.invoiceOnBehalfApprovalCreationModal.accountSearchLookup.getInput()
                    .shouldHave(exactValue(customerAccount.getName()));
            var customerPayableContactFullName = getFullName(steps.salesFlow.contact);
            opportunityPage.invoiceOnBehalfApprovalCreationModal.accountsPayableContactSearchInput.getInput()
                    .shouldHave(exactValue(customerPayableContactFullName));
            opportunityPage.invoiceOnBehalfApprovalCreationModal.partnerAccountsPayableContactSearchInput.getInput()
                    .shouldHave(exactValue(partnerAccountContactFullName));
        });

        //  CRM-23458
        step("4. Populate required fields (Approval name, invoice and payment terms, reasons for invoice, " +
                "number of users and devices, price and company information) on Approval Creation Modal", () -> {
            opportunityPage.invoiceOnBehalfApprovalCreationModal.approvalNameInput.setValue(DEFAULT_APPROVAL_NAME);
            opportunityPage.invoiceOnBehalfApprovalCreationModal.invoiceTermsPicklist.selectOption(INVOICE_TERMS);
            opportunityPage.invoiceOnBehalfApprovalCreationModal.paymentTermsPicklist.selectOption(PAYMENT_TERMS);
            opportunityPage.invoiceOnBehalfApprovalCreationModal.potentialUsersInput.setValue(POTENTIAL_USERS);
            opportunityPage.invoiceOnBehalfApprovalCreationModal.reasonCustomerRequestInvoicingInput
                    .setValue(REASON_CUSTOMER_IS_REQUESTING_INVOICE);
            opportunityPage.invoiceOnBehalfApprovalCreationModal.whyRCShouldInvoiceInput
                    .setValue(WHY_RINGCENTRAL_SHOULD_INVOICE_CUSTOMER);
            opportunityPage.invoiceOnBehalfApprovalCreationModal.initialUsersInput.setValue(INITIAL_NUMBER_OF_USERS);
            opportunityPage.invoiceOnBehalfApprovalCreationModal.initialDevicesInput.setValue(INITIAL_NUMBER_OF_DEVICES);
            opportunityPage.invoiceOnBehalfApprovalCreationModal.pricePerUserInput.setValue(PRICE_PER_USER);
            opportunityPage.invoiceOnBehalfApprovalCreationModal.companyNameInput.setValue(LEGAL_COMPANY_NAME_HEAD_OFFICE);
            opportunityPage.invoiceOnBehalfApprovalCreationModal.streetInput.setValue(ADDRESS_STREET);
            opportunityPage.invoiceOnBehalfApprovalCreationModal.zipCodeInput.setValue(ADDRESS_ZIP_CODE);
            opportunityPage.invoiceOnBehalfApprovalCreationModal.cityInput.setValue(ADDRESS_CITY);
            opportunityPage.invoiceOnBehalfApprovalCreationModal.stateInput.setValue(ADDRESS_STATE_PROVINCE);
            opportunityPage.invoiceOnBehalfApprovalCreationModal.countryInput.setValue(ADDRESS_COUNTRY);
        });

        //  CRM-23458
        step("5. Change preselected Accounts Payable Contact with 'Decision Maker' Customer Contact, " +
                "and verify that error message is shown", () -> {
            opportunityPage.invoiceOnBehalfApprovalCreationModal.accountsPayableContactSearchInput
                    .selectItemInComboboxViaSearchModal(getFullName(secondCustomerAccountContact));
            opportunityPage.invoiceOnBehalfApprovalCreationModal.getSaveButton().click();

            opportunityPage.invoiceOnBehalfApprovalCreationModal.errorsPopUpModal.getErrorsList()
                    .shouldHave(itemWithText(INCORRECT_ACCOUNTS_PAYABLE_CONTACT_ERROR), ofSeconds(30));
            opportunityPage.invoiceOnBehalfApprovalCreationModal.errorsPopUpModal.getCloseErrorListButton().click();
        });

        //  CRM-23458
        step("6. Set the valid Accounts Payable Contact again, change preselected Partner's Accounts Payable Contact " +
                "to 'Decision Maker' Partner Contact and verify that error message is shown", () -> {
            opportunityPage.invoiceOnBehalfApprovalCreationModal.accountsPayableContactSearchInput
                    .selectItemInComboboxViaSearchModal(getFullName(steps.salesFlow.contact));

            opportunityPage.invoiceOnBehalfApprovalCreationModal.partnerAccountsPayableContactSearchInput
                    .selectItemInComboboxViaSearchModal(getFullName(secondPartnerAccountContact));
            opportunityPage.invoiceOnBehalfApprovalCreationModal.getSaveButton().click();

            opportunityPage.invoiceOnBehalfApprovalCreationModal.errorsPopUpModal.getErrorsList()
                    .shouldHave(itemWithText(INCORRECT_PARTNER_ACCOUNTS_PAYABLE_CONTACT_ERROR), ofSeconds(60));
            opportunityPage.invoiceOnBehalfApprovalCreationModal.errorsPopUpModal.getCloseErrorListButton().click();
        });

        //  CRM-23382, CRM-23458
        step("7. Set the valid Partner's Accounts Payable Contact again, click 'Save' " +
                "and check that the new Invoice-on-behalf Request approval is created successfully", () -> {
            opportunityPage.invoiceOnBehalfApprovalCreationModal.partnerAccountsPayableContactSearchInput
                    .selectItemInComboboxViaSearchModal(partnerAccountContactFullName);
            opportunityPage.invoiceOnBehalfApprovalCreationModal.saveChanges();
            approvalPage.waitUntilLoaded();

            var invoiceOnBehalfApprovals = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Approval__c " +
                            "WHERE Opportunity__c = '" + steps.quoteWizard.opportunity.getId() + "' " +
                            "AND RecordType.Name = '" + INVOICE_ON_BEHALF_REQUEST_RECORD_TYPE + "'",
                    Approval__c.class);
            assertThat(invoiceOnBehalfApprovals.size())
                    .as("Number of Invoice-on-behalf Request approvals for the test Opportunity")
                    .isEqualTo(1);
        });
    }
}
