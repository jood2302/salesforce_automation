package ngbs.approvals;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Approval__c;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.Map;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.NumberHelper.doubleToInteger;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountContactRoleFactory.createAccountContactRole;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountContactRoleHelper.ACCOUNTS_PAYABLE_ROLE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.*;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.lang.String.valueOf;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("Invoice_Approval")
@Tag("Multiproduct-Lite")
public class MultiProductInvoiceApprovalLimitsFieldsTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final Map<String, Package> packageFolderNameToPackageMap;

    private final Integer signUpPurchaseLimitOfficeValue;
    private final Integer monthlyCreditLimitOfficeValue;
    private final Integer signUpPurchaseLimitRcCcValue;
    private final Integer monthlyCreditLimitRcCcValue;
    private final Integer signUpPurchaseLimitEngageDigitalValue;
    private final Integer monthlyCreditLimitEngageDigitalValue;

    public MultiProductInvoiceApprovalLimitsFieldsTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_ED_EV_CC_ProServ_Monthly_Contract.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        packageFolderNameToPackageMap = Map.of(
                data.packageFolders[0].name, data.packageFolders[0].packages[0],
                data.packageFolders[1].name, data.packageFolders[1].packages[0],
                data.packageFolders[3].name, data.packageFolders[3].packages[0]
        );

        monthlyCreditLimitOfficeValue = 1;
        signUpPurchaseLimitOfficeValue = 2;
        monthlyCreditLimitRcCcValue = 3;
        signUpPurchaseLimitRcCcValue = 4;
        monthlyCreditLimitEngageDigitalValue = 5;
        signUpPurchaseLimitEngageDigitalValue = 6;
    }

    @BeforeEach
    public void setUpTest() {
        var salesUser = steps.salesFlow.getSalesRepUser();

        steps.salesFlow.createAccountWithContactAndContactRole(salesUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesUser);

        step("Create 'Accounts Payable' AccountContactRole for the test Account via API", () -> {
            createAccountContactRole(steps.salesFlow.account, steps.salesFlow.contact, ACCOUNTS_PAYABLE_ROLE, false);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesUser);

        step("Open the Quote Wizard for the test Opportunity to add a new Sales Quote, " +
                "select Office, Engage Digital and RingCentral Contact Center packages for it, and save changes", () -> {
            steps.quoteWizard.prepareOpportunityForMultiProduct(steps.quoteWizard.opportunity.getId(), packageFolderNameToPackageMap);
        });
    }

    @Test
    @TmsLink("CRM-29042")
    @DisplayName("CRM-29042 - Multiproduct limit fields on Approval object")
    @Description("Verify that when creating Multiproduct Invoice Request, " +
            "user input for Monthly and Sign Up limit fields is saved to fields on Approval object")
    public void test() {
        step("1. Open the test Opportunity record page and and open new Approval creation modal window, " +
                "then select 'Invoicing Request' record type and open new Invoicing Request approval creation modal", () -> {
            opportunityPage.openPage(steps.quoteWizard.opportunity.getId());
            opportunityPage.openCreateNewApprovalModal();

            opportunityPage.newApprovalRecordTypeSelectionModal.selectApprovalType(INVOICING_REQUEST_RECORD_TYPE);
            opportunityPage.newApprovalRecordTypeSelectionModal.nextButton.click();

            invoiceApprovalCreationModal.approvalNameInput.shouldBe(visible, ofSeconds(30));
        });

        step("2. Check that Sign-Up Purchase Limit and Monthly Credit Limit fields are displayed for all selected services", () -> {
            invoiceApprovalCreationModal.signUpPurchaseLimitOfficeInput.shouldBe(visible);
            invoiceApprovalCreationModal.monthlyCreditLimitOfficeInput.shouldBe(visible);
            invoiceApprovalCreationModal.signUpPurchaseLimitEngageDigitalInput.shouldBe(visible);
            invoiceApprovalCreationModal.monthlyCreditLimitEngageDigitalInput.shouldBe(visible);
            invoiceApprovalCreationModal.signUpPurchaseLimitRcCcInput.shouldBe(visible);
            invoiceApprovalCreationModal.monthlyCreditLimitRcCcInput.shouldBe(visible);
        });

        step("3. Populate all required fields and MultiProduct Limits section fields on Approval Creation Modal, " +
                "and click 'Save' button", () -> {
            invoiceApprovalCreationModal.approvalNameInput.setValue(DEFAULT_APPROVAL_NAME);
            invoiceApprovalCreationModal.industryPicklist.selectOption(INDUSTRY);
            invoiceApprovalCreationModal.paymentTermsPicklist.selectOption(PAYMENT_TERMS);
            invoiceApprovalCreationModal.potentialUsersInput.setValue(POTENTIAL_USERS);
            invoiceApprovalCreationModal.reasonCustomerRequestInvoicingInput.setValue(REASON_CUSTOMER_IS_REQUESTING_INVOICE);
            invoiceApprovalCreationModal.initialDevicesInput.setValue(INITIAL_NUMBER_OF_DEVICES);
            invoiceApprovalCreationModal.companyNameInput.setValue(LEGAL_COMPANY_NAME_HEAD_OFFICE);

            invoiceApprovalCreationModal.signUpPurchaseLimitOfficeInput.setValue(valueOf(signUpPurchaseLimitOfficeValue));
            invoiceApprovalCreationModal.monthlyCreditLimitOfficeInput.setValue(valueOf(monthlyCreditLimitOfficeValue));
            invoiceApprovalCreationModal.signUpPurchaseLimitEngageDigitalInput.setValue(valueOf(signUpPurchaseLimitEngageDigitalValue));
            invoiceApprovalCreationModal.monthlyCreditLimitEngageDigitalInput.setValue(valueOf(monthlyCreditLimitEngageDigitalValue));
            invoiceApprovalCreationModal.signUpPurchaseLimitRcCcInput.setValue(valueOf(signUpPurchaseLimitRcCcValue));
            invoiceApprovalCreationModal.monthlyCreditLimitRcCcInput.setValue(valueOf(monthlyCreditLimitRcCcValue));

            invoiceApprovalCreationModal.saveChanges();
            approvalPage.waitUntilLoaded();
        });

        step("4. Check that MultiProduct Limits fields on created Invoice Approval are populated " +
                "with the same values as on Invoice Approval creation modal and limit values for Engage Voice are equal to -1", () -> {
            var invoiceApproval = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Sign_Up_Purchase_Limit_Office__c, Monthly_Credit_Limit_Office__c, " +
                            "Sign_Up_Purchase_Limit_Contact_Center__c, Monthly_Credit_Limit_Contact_Center__c, " +
                            "Sign_Up_Purchase_Limit_Engage_Digital__c, Monthly_Credit_Limit_Engage_Digital__c, " +
                            "Sign_Up_Purchase_Limit_Engage_Voice__c, Monthly_Credit_Limit_Engage_Voice__c " +
                            "FROM Approval__c " +
                            "WHERE Opportunity__c = '" + steps.quoteWizard.opportunity.getId() + "' " +
                            "AND IsMultiProductTechnicalApproval__c = false",
                    Approval__c.class);

            assertThat(doubleToInteger(invoiceApproval.getSign_Up_Purchase_Limit_Office__c()))
                    .as("Invoice Approval.Sign_Up_Purchase_Limit_Office__c value")
                    .isEqualTo(signUpPurchaseLimitOfficeValue);
            assertThat(doubleToInteger(invoiceApproval.getMonthly_Credit_Limit_Office__c()))
                    .as("Invoice Approval.Monthly_Credit_Limit_Office__c value")
                    .isEqualTo(monthlyCreditLimitOfficeValue);
            assertThat(doubleToInteger(invoiceApproval.getSign_Up_Purchase_Limit_Contact_Center__c()))
                    .as("Invoice Approval.Sign_Up_Purchase_Limit_Contact_Center__c value")
                    .isEqualTo(signUpPurchaseLimitRcCcValue);
            assertThat(doubleToInteger(invoiceApproval.getMonthly_Credit_Limit_Contact_Center__c()))
                    .as("Invoice Approval.Monthly_Credit_Limit_Contact_Center__c value")
                    .isEqualTo(monthlyCreditLimitRcCcValue);
            assertThat(doubleToInteger(invoiceApproval.getSign_Up_Purchase_Limit_Engage_Digital__c()))
                    .as("Invoice Approval.Sign_Up_Purchase_Limit_Engage_Digital__c value")
                    .isEqualTo(signUpPurchaseLimitEngageDigitalValue);
            assertThat(doubleToInteger(invoiceApproval.getMonthly_Credit_Limit_Engage_Digital__c()))
                    .as("Invoice Approval.Monthly_Credit_Limit_Engage_Digital__c value")
                    .isEqualTo(monthlyCreditLimitEngageDigitalValue);
            assertThat(doubleToInteger(invoiceApproval.getSign_Up_Purchase_Limit_Engage_Voice__c()))
                    .as("Invoice Approval.Sign_Up_Purchase_Limit_Office__c value")
                    .isEqualTo(-1);
            assertThat(doubleToInteger(invoiceApproval.getMonthly_Credit_Limit_Engage_Voice__c()))
                    .as("Invoice Approval.Sign_Up_Purchase_Limit_Office__c value")
                    .isEqualTo(-1);
        });
    }
}
