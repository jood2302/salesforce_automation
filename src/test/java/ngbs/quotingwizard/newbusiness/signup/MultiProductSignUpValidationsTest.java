package ngbs.quotingwizard.newbusiness.signup;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.codeborne.selenide.WebElementsCondition;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.Map;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.components.AreaCodeSelector.REQUIRED_AREA_CODE_ERROR;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.getSObjectIdsListAsString;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createInvoiceApprovalApproved;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createTeaApproval;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.APPROVAL_STATUS_APPROVED;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.APPROVAL_STATUS_PENDING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.ACTIVE_QUOTE_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.AGREEMENT_QUOTE_TYPE;
import static com.codeborne.selenide.CollectionCondition.*;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.switchTo;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

@Tag("P1")
@Tag("SignUp")
@Tag("Validations")
@Tag("LTR-569")
public class MultiProductSignUpValidationsTest extends BaseTest {
    private final SalesQuoteSignUpSteps salesQuoteSignUpSteps;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User salesRepUserWithPermissionSet;
    private String masterQuoteId;
    private Approval__c teaApproval;

    //  Test data
    private final Map<String, Package> packageFolderNameToPackageMap;

    public MultiProductSignUpValidationsTest() {
        salesQuoteSignUpSteps = new SalesQuoteSignUpSteps();

        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_ED_EV_CC_ProServ_Monthly_Contract.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        packageFolderNameToPackageMap = Map.of(
                data.packageFolders[0].name, data.packageFolders[0].packages[0],
                data.packageFolders[3].name, data.packageFolders[3].packages[0]
        );
    }

    @BeforeEach
    public void setUpTest() {
        salesRepUserWithPermissionSet = salesQuoteSignUpSteps.getSalesRepUserWithAllowedProcessOrderWithoutShipping();

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUserWithPermissionSet);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUserWithPermissionSet);

        salesQuoteSignUpSteps.loginAsSalesRepUserWithAllowedProcessOrderWithoutShipping();
    }

    @Test
    @TmsLink("CRM-29575")
    @TmsLink("CRM-29353")
    @TmsLink("CRM-29351")
    @TmsLink("CRM-29576")
    @TmsLink("CRM-29354")
    @TmsLink("CRM-29582")
    @DisplayName("CRM-29575 - Multiproduct - Sign Up Validation - noQuotesValidator. \n" +
            "CRM-29353 - Multiproduct - Sign Up Validation - 'No Quote Line Items' validation on Sign Up. \n" +
            "CRM-29351 - Multiproduct - Sign Up Validation - Area Codes validation on Sign Up. \n" +
            "CRM-29576 - Multiproduct - Sign Up Validation - quoteHasErrorsValidator. \n" +
            "CRM-29354 - Multiproduct - Sign Up Validation - 'Active Agreement' validation on Sign Up. \n" +
            "CRM-29582 - Multiproduct - Sign Up Validation - pendingTaxExemptionApprovalValidator")
    @Description("CRM-29575 - Verify that for Opportunity with no quotes an Error message is shown for sign up. \n" +
            "CRM-29353 - Verify that 'No Quote Line Items' validation is present on Process Order button for Multiproduct Opportunities. \n" +
            "CRM-29351 - Verify that Main and Fax area code validations are present on Process Order button for Multiproduct Opportunities. \n" +
            "CRM-29576 - Verify that for Opportunity with errors an Error message is shown for sign up. \n" +
            "CRM-29354 - Verify that 'Active Agreement' validation is present on Process Order button for Multiproduct Opportunities. \n" +
            "CRM-29582 - Verify that for Opportunity with pending Tax Exempt Approval a warning message is shown for sign up")
    public void test() {
        //  CRM-29575
        step("1. Open the Opportunity record page, click 'Process Order' button " +
                "and check error notification about required quote for Sign Up", () -> {
            opportunityPage.openPage(steps.quoteWizard.opportunity.getId());

            checkValidationErrorOnProcessOrderModal(exactTextsCaseSensitive(QUOTE_IS_REQUIRED_TO_SIGNUP_ERROR));
        });

        step("2. Switch to the Quote Wizard, add a new Sales Quote, select MVP and Engage Digital packages for it " +
                "and save changes", () -> {
            opportunityPage.switchToNGBSQW();
            steps.quoteWizard.addNewSalesQuote();
            steps.quoteWizard.selectPackagesForMultiProductQuote(packageFolderNameToPackageMap);
            packagePage.saveChanges();

            masterQuoteId = wizardPage.getSelectedQuoteId();
        });

        //  CRM-29351
        step("3. Open the Quote Details tab and check the error messages indicating that both Area Codes are missing", () -> {
            quotePage.openTab();

            quotePage.mainAreaCodeError.shouldHave(exactTextCaseSensitive(REQUIRED_AREA_CODE_ERROR), ofSeconds(15));
            quotePage.faxAreaCodeError.shouldHave(exactTextCaseSensitive(REQUIRED_AREA_CODE_ERROR), ofSeconds(15));
        });

        step("4. Create new Invoice Request Approval with Status = 'Approved' via API", () ->
                createInvoiceApprovalApproved(steps.quoteWizard.opportunity, steps.salesFlow.account, steps.salesFlow.contact,
                        salesRepUserWithPermissionSet.getId(), true)
        );

        //  CRM-29351
        step("5. Switch to the Opportunity record page, click 'Process Order' button " +
                "and check that error notification about missing Area Codes is present", () -> {
            checkValidationErrorOnProcessOrderModal(itemWithText(format(AREA_CODES_ARE_MISSING_ERROR, MVP_SERVICE)));
        });

        step("6. Switch back to the Quote Wizard, set Start Date and Main Area Code, save changes, " +
                "select Quote Type = 'Agreement' and save changes", () -> {
            switchTo().window(1);
            quotePage.setDefaultStartDate();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.saveChanges();

            quotePage.stagePicklist.selectOption(AGREEMENT_QUOTE_TYPE);
            quotePage.saveChanges();
        });

        //  CRM-29354
        step("7. Switch to the Opportunity record page, click 'Process Order' button " +
                "and check error notifications about the required Active Agreement for all services on the Quote", () -> {
            checkValidationErrorOnProcessOrderModal(exactTextsCaseSensitive(
                    format(ACTIVE_AGREEMENT_IS_REQUIRED_TO_SIGN_UP_ERROR, MVP_SERVICE),
                    format(YOU_NEED_AN_ACTIVE_AGREEMENT_TO_SIGN_UP_ERROR, MVP_SERVICE),
                    format(ACTIVE_AGREEMENT_IS_REQUIRED_TO_SIGN_UP_ERROR, ENGAGE_DIGITAL_SERVICE),
                    format(YOU_NEED_AN_ACTIVE_AGREEMENT_TO_SIGN_UP_ERROR, ENGAGE_DIGITAL_SERVICE)));
        });

        step("8. Set Status = 'Active' and IsQuoteHasErrors__c = true on the Master Quote via API", () -> {
            var masterQuoteToUpdate = new Quote();
            masterQuoteToUpdate.setId(masterQuoteId);
            //  in order to avoid the error message about the required Active Agreement
            masterQuoteToUpdate.setStatus(ACTIVE_QUOTE_STATUS);
            masterQuoteToUpdate.setIsQuoteHasErrors__c(true);
            enterpriseConnectionUtils.update(masterQuoteToUpdate);
        });

        //  CRM-29576
        step("9. Click 'Process Order' button " +
                "and check error notifications about the errors on the Primary Quote for all services", () -> {
            checkValidationErrorOnProcessOrderModal(exactTextsCaseSensitive(
                    format(PRIMARY_QUOTE_HAS_ERRORS_ERROR, MVP_SERVICE),
                    format(PRIMARY_QUOTE_HAS_ERRORS_ERROR, ENGAGE_DIGITAL_SERVICE)));
        });

        step("10. Set IsQuoteHasErrors__c = false on the Master Quote via API", () -> {
            var masterQuoteToUpdate = new Quote();
            masterQuoteToUpdate.setId(masterQuoteId);
            masterQuoteToUpdate.setIsQuoteHasErrors__c(false);
            enterpriseConnectionUtils.update(masterQuoteToUpdate);
        });

        step("11. Create a new Tax Exemption Approval for the test Opportunity " +
                "and set its Status to 'Pending Approval' (all via API)", () -> {
            teaApproval = createTeaApproval(steps.salesFlow.account.getId(), steps.quoteWizard.opportunity.getId(),
                    steps.salesFlow.contact.getId(), salesRepUserWithPermissionSet.getId());

            teaApproval.setStatus__c(APPROVAL_STATUS_PENDING);
            enterpriseConnectionUtils.update(teaApproval);
        });

        //  CRM-29582
        step("12. Click 'Process Order' button " +
                "and check warning notifications in the Process Order modal window", () -> {
            opportunityPage.clickProcessOrderButton();

            opportunityPage.processOrderModal.waitUntilMvpPreparingDataStepIsCompleted();
            opportunityPage.processOrderModal.alertNotificationBlock.click();
            opportunityPage.processOrderModal.warningNotifications
                    .shouldHave(exactTexts(
                            format(PENDING_TEA_APPROVALS_WARNING, MVP_SERVICE),
                            format(PENDING_TEA_APPROVALS_WARNING, ENGAGE_DIGITAL_SERVICE)));
            opportunityPage.processOrderModal.closeWindow();
        });

        step("13. Set the Tax Exemption Approval Status to 'Approved' via API", () -> {
            teaApproval.setStatus__c(APPROVAL_STATUS_APPROVED);
            enterpriseConnectionUtils.update(teaApproval);
        });

        //  CRM-29582
        step("14. Click 'Process Order' button and check that there are no notifications in the Process Order modal window", () -> {
            opportunityPage.clickProcessOrderButton();

            opportunityPage.processOrderModal.waitUntilMvpPreparingDataStepIsCompleted();
            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(hidden);
            opportunityPage.processOrderModal.closeWindow();
        });

        step("15. Delete all the QuoteLineItem records from the Master and Technical Quotes via API", () -> {
            var masterQuoteLineItems = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM QuoteLineItem " +
                            "WHERE QuoteId = '" + masterQuoteId + "' ",
                    QuoteLineItem.class);
            var techQuoteLineItems = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM QuoteLineItem " +
                            "WHERE MasterQLI__c IN " + getSObjectIdsListAsString(masterQuoteLineItems),
                    QuoteLineItem.class);

            enterpriseConnectionUtils.delete(masterQuoteLineItems);
            enterpriseConnectionUtils.delete(techQuoteLineItems);
        });

        //  CRM-29353
        step("16. Click 'Process Order' button " +
                "and check error notifications about missing QuoteLineItems on the Quote for each service", () -> {
            checkValidationErrorOnProcessOrderModal(exactTextsCaseSensitive(
                    format(PLEASE_ADD_ITEMS_IN_CART_ERROR, MVP_SERVICE),
                    format(PLEASE_ADD_ITEMS_IN_CART_ERROR, ENGAGE_DIGITAL_SERVICE)));
        });
    }

    /**
     * Check that the provided validation error notifications are shown on the Process Order modal window.
     *
     * @param condition the condition that contains necessary errors to check
     */
    private void checkValidationErrorOnProcessOrderModal(WebElementsCondition condition) {
        opportunityPage.clickProcessOrderButton();

        opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(visible, ofSeconds(60)).click();
        opportunityPage.processOrderModal.errorNotifications.shouldHave(condition);
        opportunityPage.processOrderModal.closeWindow();
    }
}
