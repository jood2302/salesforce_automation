package ngbs.quotingwizard.newbusiness.opportunityclose;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.OpportunityRecordPage.MONTHLY_CREDIT_LIMIT_EXCEEDED_ERROR;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage.YOU_DO_NOT_HAVE_APPROVED_DIRECT_DEBIT_REQUEST_APPROVAL;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.DIRECT_DEBIT_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.page.salesforce.approval.ApprovalHistoryRelatedListPage.*;
import static com.aquiva.autotests.rc.page.salesforce.approval.ApprovalPage.APPROVAL_HISTORY_RELATED_LIST;
import static com.aquiva.autotests.rc.utilities.Constants.USER;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.CollectionCondition.itemWithText;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("DirectDebit")
public class DirectDebitCloseValidationTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private final List<String> assignedToWithApprovedSteps;
    private User salesRepUser;
    private String quoteId;
    private String directDebitRequestApprovalId;
    private String mainUserAdminFullName;

    //  Test data
    private final Product digitalLineUnlimited;
    private final Double newDlPrice;
    private final AreaCode euAreaCode;

    public DirectDebitCloseValidationTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_EU_MVP_Monthly_Contract_RegularAndPOC.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        digitalLineUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        newDlPrice = digitalLineUnlimited.priceRater[0].raterPrice.doubleValue();
        euAreaCode = new AreaCode("Local", "France", EMPTY_STRING, EMPTY_STRING, "1");

        assignedToWithApprovedSteps = new ArrayList<>();
    }

    @BeforeEach
    public void setUpTest() {
        salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-23946")
    @TmsLink("CRM-37373")
    @DisplayName("CRM-23946 - New Business. Opportunity can't be closed with Direct Debit PM and Threshold Limits violated. Monthly. \n" +
            "CRM-37373 - Direct Debit Approval Process")
    @Description("CRM-23946 - Verify that Opportunity can't be closed with Direct Debit PM and Package Limits violated. \n" +
            "CRM-37373 - Verify that Direct Debit Approval can be created and Approved")
    public void test() {
        step("1. Open the Opportunity record page, switch to the Quote Wizard, " +
                "add a new Sales Quote, and select a package for it", () -> {
            steps.quoteWizard.openQuoteWizardOnOpportunityRecordPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addNewSalesQuote();
            steps.quoteWizard.selectDefaultPackageFromTestData();
        });

        step("2. Open the Price tab and increase the quantity of DLs so that Quote.Total_MRR_with_Taxes__c value " +
                "is just above the DirectDebitApprovalThreshold__c value for the brand, and save changes", () -> {
            cartPage.openTab();

            var directDebitThresholdValue = enterpriseConnectionUtils.querySingleRecord(
                            "SELECT Id, ThresholdValue__c " +
                                    "FROM DirectDebitApprovalThreshold__c " +
                                    "WHERE Brand__c = '" + data.brandName + "'",
                            DirectDebitApprovalThreshold__c.class)
                    .getThresholdValue__c();
            assertThat(directDebitThresholdValue)
                    .as("DirectDebitApprovalThreshold__c.ThresholdValue__c value")
                    .isNotNull();

            var dlQuantityAboveThreshold = (int) Math.ceil(directDebitThresholdValue / newDlPrice);
            cartPage.setQuantityForQLItem(digitalLineUnlimited.name, dlQuantityAboveThreshold);
            cartPage.saveChanges();

            //  make sure that the totals exceed DD Approval threshold
            var quote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Total_MRR_with_Taxes__c " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "'",
                    Quote.class);
            assertThat(quote.getTotal_MRR_with_Taxes__c())
                    .as("Quote.Total_MRR_with_Taxes__c value")
                    .isGreaterThan(directDebitThresholdValue);

            quoteId = quote.getId();
        });

        step("3. Open the Quote Details tab, set Payment Method = 'Direct Debit', set Main Area Code, save changes, " +
                "and check that notification about required Direct Debit Approval appeared in the Quote Wizard", () -> {
            quotePage.openTab();
            quotePage.selectPaymentMethod(DIRECT_DEBIT_PAYMENT_METHOD);
            quotePage.setMainAreaCode(euAreaCode);
            quotePage.saveChanges();

            cartPage.openTab();
            //  to make all notifications displayed
            cartPage.notificationBar.click();
            cartPage.notifications.shouldHave(itemWithText(YOU_DO_NOT_HAVE_APPROVED_DIRECT_DEBIT_REQUEST_APPROVAL));
        });

        //  CRM-23946
        step("4. Click 'Close' button on the Opportunity record page " +
                "and check that error notification about exceeded Monthly credit limit is appeared", () -> {
            opportunityPage.clickCloseButton();

            opportunityPage.alertNotificationBlock.shouldBe(visible, ofSeconds(40));
            opportunityPage.notifications.shouldHave(itemWithText(MONTHLY_CREDIT_LIMIT_EXCEEDED_ERROR), ofSeconds(1));

            opportunityPage.closeOpportunityModal.closeWindow();
        });

        step("5. Open the Direct Debit Request approval creation modal via Approval section on the Opportunity page", () -> {
            opportunityPage.openCreateNewApprovalModal();
            opportunityPage.newApprovalRecordTypeSelectionModal.selectApprovalType(DIRECT_DEBIT_APPROVAL_RECORD_TYPE);
            opportunityPage.newApprovalRecordTypeSelectionModal.nextButton.click();

            directDebitApprovalCreationModal.approvalNameInput.shouldBe(visible, ofSeconds(20));
        });

        //  CRM-37373
        step("6. Populate all the necessary fields on the form and save changes", () -> {
            directDebitApprovalCreationModal.approvalNameInput.setValue(DEFAULT_APPROVAL_NAME);
            directDebitApprovalCreationModal.accountsPayableEmailAddressInput.setValue(ACCOUNTS_PAYABLE_EMAIL_ADDRESS);
            directDebitApprovalCreationModal.potentialUsersInput.setValue(POTENTIAL_USERS);
            directDebitApprovalCreationModal.initialUsersInput.setValue(INITIAL_NUMBER_OF_USERS);
            directDebitApprovalCreationModal.initialDevicesInput.setValue(INITIAL_NUMBER_OF_DEVICES);
            directDebitApprovalCreationModal.pricePerUserInput.setValue(PRICE_PER_USER);

            directDebitApprovalCreationModal.companyNameInput.setValue(LEGAL_COMPANY_NAME_HEAD_OFFICE);
            directDebitApprovalCreationModal.streetInput.setValue(ADDRESS_STREET);
            directDebitApprovalCreationModal.zipCodeInput.setValue(ADDRESS_ZIP_CODE);
            directDebitApprovalCreationModal.cityInput.setValue(ADDRESS_CITY);
            directDebitApprovalCreationModal.stateInput.setValue(ADDRESS_STATE_PROVINCE);
            directDebitApprovalCreationModal.countryInput.setValue(ADDRESS_COUNTRY);

            directDebitApprovalCreationModal.saveChanges();
            approvalPage.waitUntilLoaded();

            directDebitRequestApprovalId = approvalPage.getCurrentRecordId();
        });

        step("7. Submit the created Direct Debit Request for Approval via API", () -> {
            enterpriseConnectionUtils.submitRecordForApproval(directDebitRequestApprovalId);
        });

        //  CRM-37373
        step("8. Open the Approval History related list and check the displayed steps", () -> {
            approvalPage.openRelatedList(APPROVAL_HISTORY_RELATED_LIST);

            //  The record is submitted for the approval via SFDC API which means via the main Admin user
            var mainUser = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Name " +
                            "FROM User " +
                            "WHERE Username = '" + USER + "'",
                    User.class);
            mainUserAdminFullName = mainUser.getName();

            var initialStep = approvalPage.approvalHistoryRelatedListPage.getStep(mainUserAdminFullName);
            initialStep.getAssignedTo().shouldHave(exactTextCaseSensitive(mainUserAdminFullName));
            initialStep.getStatus().shouldHave(exactTextCaseSensitive(SUBMITTED_STATUS));

            var nextStep = approvalPage.approvalHistoryRelatedListPage.getStep(FINANCE_INVOICE_APPROVAL_GROUP);
            nextStep.getAssignedTo().shouldHave(exactTextCaseSensitive(FINANCE_INVOICE_APPROVAL_GROUP));
            nextStep.getStatus().shouldHave(exactTextCaseSensitive(PENDING_STATUS));
        });

        //  CRM-37373
        step("9. Re-login as a user from 'Finance Invoice Approval' group, " +
                "open the Approval History page for the Direct Debit Request Approval, " +
                "approve the record on behalf of the user, and check the displayed steps", () -> {
            var financeInvoiceApprovalGroupUser = getUser()
                    .withProfile(FINANCE_ADMIN_EU_RESTRICTED_PROFILE)
                    .withGroupMembership(FINANCE_INVOICE_APPROVAL_GROUP)
                    .withLimit(1)
                    .getAllUsersWithCurrentFilter()
                    .get(0);
            reLoginAsCurrentApproverAndApprove(financeInvoiceApprovalGroupUser,
                    FINANCE_INVOICE_APPROVAL_GROUP, TREASURY_CREDIT_CHECK_GROUP);
        });

        //  CRM-37373
        step("10. Re-login as a user from 'Treasury Credit Check' group, " +
                "open the Approval History page for the Direct Debit Request Approval, " +
                "approve the record on behalf of the user, and check the displayed steps", () -> {
            var treasuryCreditCheckUser = getUser()
                    .withProfile(FINANCE_ADMIN_EU_RESTRICTED_PROFILE)
                    .withGroupMembership(TREASURY_CREDIT_CHECK_GROUP)
                    .withLimit(1)
                    .getAllUsersWithCurrentFilter()
                    .get(0);
            reLoginAsCurrentApproverAndApprove(treasuryCreditCheckUser,
                    TREASURY_CREDIT_CHECK_GROUP, D_N_B_CREDIT_CHECK_USER_FULL_NAME);
        });

        //  CRM-37373
        step("11. Re-login as a user with Name = 'D&B Credit Check', " +
                "open the Approval History page for the Direct Debit Request Approval, " +
                "approve the record on behalf of the user, and check the displayed steps", () -> {
            var dnbCreditCheckUser = getUser()
                    .withFullNames(List.of(D_N_B_CREDIT_CHECK_USER_FULL_NAME))
                    .withLimit(1)
                    .getAllUsersWithCurrentFilter()
                    .get(0);
            reLoginAsCurrentApproverAndApprove(dnbCreditCheckUser,
                    D_N_B_CREDIT_CHECK_USER_FULL_NAME, INVOICE_APPROVAL_QUEUE_GROUP);
        });

        //  CRM-37373
        step("12. Re-login as a user from 'Invoice Approval Queue' group, " +
                "and open the Approval History page for the Direct Debit Request Approval", () -> {
            var invoiceApprovalQueueUser = getUser()
                    .withProfile(FINANCE_ADMIN_EU_RESTRICTED_PROFILE)
                    .withGroupMembership(INVOICE_APPROVAL_QUEUE_GROUP)
                    .withLimit(1)
                    .getAllUsersWithCurrentFilter()
                    .get(0);
            steps.sfdc.reLoginAsUser(invoiceApprovalQueueUser);

            approvalPage.approvalHistoryRelatedListPage.openPage(directDebitRequestApprovalId);
        });

        //  CRM-37373
        step("13. Approve the record on behalf of the user, " +
                "check the displayed steps and the Direct Debit Request record's Status__c value", () -> {
            approvalPage.approvalHistoryRelatedListPage.approveApprovalRequest();
            assignedToWithApprovedSteps.add(INVOICE_APPROVAL_QUEUE_GROUP);

            var initialStep = approvalPage.approvalHistoryRelatedListPage.getStep(mainUserAdminFullName);
            initialStep.getStatus().shouldHave(exactTextCaseSensitive(SUBMITTED_STATUS));

            for (var assignedTo : assignedToWithApprovedSteps) {
                approvalPage.approvalHistoryRelatedListPage
                        .getStep(assignedTo)
                        .getStatus()
                        .shouldHave(exactTextCaseSensitive(APPROVED_STATUS));
            }

            var updatedDirectDebitRequest = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Status__c " +
                            "FROM Approval__c " +
                            "WHERE Id = '" + directDebitRequestApprovalId + "'",
                    Approval__c.class);
            assertThat(updatedDirectDebitRequest.getStatus__c())
                    .as("Direct Debit Request Approval__c.Status__c value")
                    .isEqualTo(APPROVAL_STATUS_APPROVED);
        });

        //  CRM-37373
        step("14. Re-login as a user with 'Sales Rep - Lightning' profile, " +
                "open the Quote in the Quote Wizard, open the Price tab, " +
                "and check that there are NO displayed notifications in the Notification bar", () -> {
            steps.sfdc.reLoginAsUser(salesRepUser);

            wizardPage.openPage(steps.quoteWizard.opportunity.getId(), quoteId);
            cartPage.openTab();
            cartPage.notificationBar.shouldBe(hidden);
        });
    }

    /**
     * Re-login as one of the users eligible for the approval of the Direct Debit Request,
     * approve the record on behalf of the user,
     * and check the steps in the Approval History related list.
     *
     * @param currentApproverUser     User that should currently approve the record
     * @param currentApproverAssignee Name of the user or a User's Group that should currently approve the record
     * @param nextApproverAssignee    Name of the user or a User's Group that should approve the record next
     */
    private void reLoginAsCurrentApproverAndApprove(User currentApproverUser,
                                                    String currentApproverAssignee, String nextApproverAssignee) {
        step("Re-login as one of the current approver users, " +
                "and open the Approval History page for the Direct Debit Request Approval", () -> {
            steps.sfdc.reLoginAsUser(currentApproverUser);

            approvalPage.approvalHistoryRelatedListPage.openPage(directDebitRequestApprovalId);
        });

        step("Approve the record on behalf of the user, and check the displayed steps", () -> {
            approvalPage.approvalHistoryRelatedListPage.approveApprovalRequest();
            assignedToWithApprovedSteps.add(currentApproverAssignee);

            var newApprovedStep = approvalPage.approvalHistoryRelatedListPage.getStep(currentApproverAssignee);
            newApprovedStep.getAssignedTo().shouldHave(exactTextCaseSensitive(currentApproverAssignee));
            newApprovedStep.getStatus().shouldHave(exactTextCaseSensitive(APPROVED_STATUS));

            var nextStep = approvalPage.approvalHistoryRelatedListPage.getStep(nextApproverAssignee);
            nextStep.getAssignedTo().shouldHave(exactTextCaseSensitive(nextApproverAssignee));
            nextStep.getStatus().shouldHave(exactTextCaseSensitive(PENDING_STATUS));
        });
    }
}
