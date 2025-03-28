package ngbs.approvals;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.kycApprovalPage;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.refresh;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

@Tag("P1")
@Tag("IndiaMVP")
public class GstStateCodeOfCustomerPopulationOnKycApprovalTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User salesUserWithEditKycApprovalPermissionSet;
    private Approval__c kycApproval;

    //  Test data
    private final String indiaBillingState;
    private final String indiaBillingCity;
    private final String indiaBillingStreet;
    private final String indiaBillingPostalCode;
    private final String indiaGstStateCodeValue;

    private final String indiaBillingStateNew;
    private final String indiaGstStateCodeValueNew;

    public GstStateCodeOfCustomerPopulationOnKycApprovalTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_IndiaAndUS_MVP_Monthly_Contract.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        indiaBillingState = "MH";
        indiaBillingCity = "Mumbai";
        indiaBillingStreet = "A/85, Deewan Shopping Cent";
        indiaBillingPostalCode = "400102";
        indiaGstStateCodeValue = "27";

        indiaBillingStateNew = "Andhra Pradesh";
        indiaGstStateCodeValueNew = "37";
    }

    @BeforeEach
    public void setUpTest() {
        salesUserWithEditKycApprovalPermissionSet = steps.kycApproval.getSalesUserWithKycApprovalPermissionSet();

        steps.salesFlow.createAccountWithContactAndContactRole(salesUserWithEditKycApprovalPermissionSet);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact,
                salesUserWithEditKycApprovalPermissionSet);

        kycApproval = steps.kycApproval.changeOwnerOfDefaultKycApproval(
                salesUserWithEditKycApprovalPermissionSet.getId(), steps.salesFlow.account.getId());

        step("Clear Billing Address on the Account via API", () -> {
            var accountToUpdate = new Account();
            accountToUpdate.setId(steps.salesFlow.account.getId());
            accountToUpdate.setFieldsToNull(new String[]{
                    "BillingCountry", "BillingCity", "BillingStreet", "BillingState", "BillingPostalCode"
            });

            enterpriseConnectionUtils.update(accountToUpdate);
        });

        step("Login as a user with 'Sales Rep - Lightning' profile with 'KYC_Approval_Edit' permission set", () ->
                steps.sfdc.initLoginToSfdcAsTestUser(salesUserWithEditKycApprovalPermissionSet)
        );
    }

    @Test
    @TmsLink("CRM-24177")
    @DisplayName("CRM-24177 - Populating GST State Code of Customer")
    @Description("Verify that 'GST State Code of Customer' field is always readonly and depends on the state value")
    public void test() {
        step("1. Open India Account's KYC Approval page", () -> {
            kycApprovalPage.openPage(kycApproval.getId());
        });

        step("2. Switch to 'KYC Details' block, expand 'GST No. & Certificate' section, " +
                "and check that 'GST State Code Of Customer' is empty and disabled", () -> {
            kycApprovalPage.gstNumberSection.click();
            kycApprovalPage.gstStateCodeOfCustomerInput.shouldBe(empty, disabled);
        });

        step("3. Populate Billing Address section fields, save changes " +
                "and check 'GST State Code of Customer' field value and state", () -> {
            steps.kycApproval.populateBillingAddressSectionWithIndiaAddress(indiaBillingStreet, indiaBillingCity,
                    indiaBillingState, indiaBillingPostalCode);
            kycApprovalPage.kycDetailsSaveChanges();

            kycApprovalPage.gstStateCodeOfCustomerInput
                    .shouldHave(exactValue(indiaGstStateCodeValue))
                    .shouldBe(disabled);
        });

        step("4. Update State field in the Billing Address section with '" + indiaBillingStateNew + "' value, " +
                "save changes and check GST State Code of Customer value and state", () -> {
            kycApprovalPage.billingStateInput.setValue(indiaBillingStateNew);
            kycApprovalPage.kycDetailsSaveChanges();

            kycApprovalPage.gstStateCodeOfCustomerInput
                    .shouldHave(exactValue(indiaGstStateCodeValueNew))
                    .shouldBe(disabled);
        });

        step("5. Refresh the page and check that GST State Code of Customer field value has the same value " +
                "on KYC Approval page in 'Financial / Tax Details' section", () -> {
            refresh();
            kycApprovalPage.gstStateCodeOfCustomerOutput.shouldHave(exactText(indiaGstStateCodeValueNew), ofSeconds(20));
        });
    }
}
