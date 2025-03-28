package ngbs.opportunitycreation.existingbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.opportunityCreationPage;
import static base.Pages.opportunityPage;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage.QUOTING_IS_UNAVAILABLE_MESSAGE;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

@Tag("P0")
@Tag("QOP")
@Tag("Verizon")
public class VerizonOpportunityCreationTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User salesUserWithVerizonPermissionSet;

    //  Test data
    private final String rcVerizonBrand;

    public VerizonOpportunityCreationTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/opportunitycreation/existingbusiness/RC_Verizon_Monthly_NonContract_84610013.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        rcVerizonBrand = data.brandName;
    }

    @BeforeEach
    public void setUpTest() {
        steps.ngbs.generateBillingAccount();

        step("Find a user with 'Sales Rep - Lightning' profile and 'RingCentral_with_Verizon' permission set", () -> {
            salesUserWithVerizonPermissionSet = getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    .withPermissionSet(RINGCENTRAL_WITH_VERIZON_PS)
                    //  to avoid issues with records sharing during the Opportunity creation
                    .withGroupMembership(NON_GSP_GROUP)
                    .execute();
        });

        steps.salesFlow.createAccountWithContactAndContactRole(salesUserWithVerizonPermissionSet);

        step("Set Account.RC_Brand__c = 'RingCentral with Verizon' via API", () -> {
            steps.salesFlow.account.setRC_Brand__c(rcVerizonBrand);
            enterpriseConnectionUtils.update(steps.salesFlow.account, steps.salesFlow.contact);
        });

        step("Login as a user with 'Sales Rep - Lightning' profile and 'RingCentral_with_Verizon' permission set", () ->
                steps.sfdc.initLoginToSfdcAsTestUser(salesUserWithVerizonPermissionSet)
        );
    }

    @Test
    @TmsLink("CRM-25237")
    @DisplayName("CRM-25237 - Quoting is unavailable for Verizon (Existing Business)")
    @Description("Verify that quoting is not available for Existing Business Accounts with the 'RingCentral with Verizon' Brand")
    public void test() {
        step("1. Open QOP for the test Account", () ->
                opportunityCreationPage.openPage(steps.salesFlow.account.getId())
        );

        step("2. Populate Close Date and Provisioning Details fields", () -> {
            opportunityCreationPage.populateCloseDate();
            opportunityCreationPage.provisioningDetailsTextArea.setValue(TEST_STRING);
        });

        step("3. Click 'Continue to Opportunity' button", () -> {
            opportunityCreationPage.billingSystem.shouldBe(visible, ofSeconds(20));

            steps.opportunityCreation.pressContinueToOpp();
        });

        step("4. Check that there's notification message instead of the Quote Wizard " +
                "on the Quote tab of Opportunity record page", () -> {
            opportunityPage.switchToNGBSQWIframeWithoutQuote();
            opportunityPage.wizardBodyPage.wizardPlaceholder
                    .shouldHave(exactTextCaseSensitive(QUOTING_IS_UNAVAILABLE_MESSAGE), ofSeconds(10));
        });
    }
}
