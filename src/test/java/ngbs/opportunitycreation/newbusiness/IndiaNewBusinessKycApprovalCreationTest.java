package ngbs.opportunitycreation.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.INVOICE_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.NONE_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.APPROVAL_STATUS_PENDING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.KYC_APPROVAL_RECORD_TYPE;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("P1")
@Tag("Approval")
@Tag("IndiaMVP")
@Tag("QuoteTab")
public class IndiaNewBusinessKycApprovalCreationTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User salesRepUserWithEditKycApprovalPS;

    //  Test data
    private final String chargeTerm;
    private final String packageFolderName;
    private final Package indiaPackage;
    private final List<String> availablePaymentMethods;

    public IndiaNewBusinessKycApprovalCreationTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/opportunitycreation/newbusiness/RC_India_Mumbai_MVP_Monthly_Contract_QOP.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        chargeTerm = data.chargeTerm;
        packageFolderName = data.packageFolders[0].name;
        indiaPackage = data.packageFolders[0].packages[0];

        availablePaymentMethods = List.of(INVOICE_PAYMENT_METHOD, NONE_PAYMENT_METHOD);
    }

    @BeforeEach
    public void setUpTest() {
        salesRepUserWithEditKycApprovalPS = steps.kycApproval.getSalesUserWithKycApprovalPermissionSet();

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUserWithEditKycApprovalPS);

        step("Login as a user with 'Sales Rep - Lightning' profile and 'KYC_Approval_Edit' permission set", () -> {
            steps.sfdc.initLoginToSfdcAsTestUser(salesRepUserWithEditKycApprovalPS);
        });
    }

    @Test
    @TmsLink("CRM-23629")
    @TmsLink("CRM-23578")
    @TmsLink("CRM-23871")
    @DisplayName("CRM-23629 - KYC Approval autocreation in QOP. \n" +
            "CRM-23578 - 'Special Terms' availability on the India quotes. \n" +
            "CRM-23871 - Values for 'Intended Payment Method' field")
    @Description("CRM-23629 - Verify that KYC Approval is created automatically in QOP. \n " +
            "CRM-23578 - Verify that 'Special Terms' selector is not displayed on the 'Quote' tab for India quotes. \n" +
            "CRM-23871 - Verify that only 'Invoice' is available value for the Intended Payment Method field")
    public void test() {
        step("1. Open Quick Opportunity creation Page (QOP)", () -> {
            opportunityCreationPage.openPage(steps.salesFlow.account.getId());
        });

        step("2. Select Business Identity for Opportunity, and populate Close Date and Provisioning Details fields", () -> {
            opportunityCreationPage.businessIdentityPicklist
                    .shouldBe(enabled, ofSeconds(60))
                    .selectOption(data.getBusinessIdentityName());
            opportunityCreationPage.populateCloseDate();
            opportunityCreationPage.provisioningDetailsTextArea.setValue(TEST_STRING);
        });

        step("3. Click 'Continue to Opportunity' button", () ->
                steps.opportunityCreation.pressContinueToOpp()
        );

        step("4. Switch to the Quote Wizard on the Opportunity record page, " +
                "add new Sales Quote, select a package, and save changes", () -> {
            opportunityPage.switchToNGBSQW();
            steps.quoteWizard.addNewSalesQuote();
            packagePage.packageSelector.selectPackage(chargeTerm, packageFolderName, indiaPackage);
            packagePage.saveChanges();
        });

        step("5. Open the Quote Details tab, check 'Payment Method' picklist values " +
                "and 'Special Terms' picklist visibility", () -> {
            quotePage.openTab();

            //  CRM-23871
            quotePage.paymentMethodPicklist.getOptions().shouldHave(exactTextsCaseSensitiveInAnyOrder(availablePaymentMethods));
            //  CRM-23578
            quotePage.footer.billingDetailsAndTermsButton.click();

            quotePage.billingDetailsAndTermsModal.specialTermsPicklist.shouldBe(disabled);
        });

        step("6. Check created KYC Approval", () -> {
            var kycApproval = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Name, Account__c, Opportunity__c, Status__c " +
                            "FROM Approval__c " +
                            "WHERE Account__c = '" + steps.salesFlow.account.getId() + "'",
                    Approval__c.class);
            var createdOpportunity = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM Opportunity " +
                            "WHERE AccountId = '" + steps.salesFlow.account.getId() + "'",
                    Opportunity.class);

            //  CRM-23629
            assertThat(kycApproval.getStatus__c())
                    .as("Approval__c.Status__c value")
                    .isEqualTo(APPROVAL_STATUS_PENDING);
            assertThat(kycApproval.getOpportunity__c())
                    .as("Approval__c.Opportunity__c value")
                    .isEqualTo(createdOpportunity.getId());
            assertThat(kycApproval.getAccount__c())
                    .as("Approval__c.Account__c value")
                    .isEqualTo(steps.salesFlow.account.getId());
            assertThat(kycApproval.getName())
                    .as("Approval__c.Name value")
                    .isEqualTo(format("%s - %s", KYC_APPROVAL_RECORD_TYPE, steps.salesFlow.account.getName()));
        });
    }
}
