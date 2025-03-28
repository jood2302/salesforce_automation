package leads.leadconvertpage.businessidentities;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.leadConvertPage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

@Tag("P0")
@Tag("LeadConvert")
@Tag("Verizon")
public class VerizonBiLeadConvertTest extends BaseTest {
    private final Steps steps;

    private User salesUserWithVerizonPermissionSet;

    //  Test data
    private final String verizonBiName;

    public VerizonBiLeadConvertTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/RC_Verizon_US_Monthly_NonContract.json",
                Dataset.class);
        steps = new Steps(data);

        verizonBiName = data.getBusinessIdentityName();
    }

    @BeforeEach
    public void setUpTest() {
        step("Find a user with 'Sales Rep - Lightning' profile and 'RingCentral_with_Verizon' permission set", () -> {
            salesUserWithVerizonPermissionSet = getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    .withPermissionSet(RINGCENTRAL_WITH_VERIZON_PS)
                    .execute();
        });

        steps.leadConvert.createPartnerAccountAndLead(salesUserWithVerizonPermissionSet);
        steps.salesFlow.createAccountWithContactAndContactRole(salesUserWithVerizonPermissionSet);
        steps.leadConvert.preparePartnerLeadTestSteps(steps.salesFlow.account, steps.salesFlow.contact);

        step("Login as a user with 'Sales Rep - Lightning' profile and 'RingCentral_with_Verizon' permission set", () -> {
            steps.sfdc.initLoginToSfdcAsTestUser(salesUserWithVerizonPermissionSet);
        });
    }

    @Test
    @TmsLink("CRM-26129")
    @DisplayName("CRM-26129 - Preselected BI on LC page for Verizon")
    @Description("Verify that RingCentral with Verizon BI is preselected on LC Page if Lead_Brand_Name__c = RingCentral with Verizon")
    public void test() {
        step("1. Open Lead Convert page for the Partner Lead", () -> {
            leadConvertPage.openPage(steps.leadConvert.partnerLead.getId());
        });

        step("2. Select 'Create a new account' toggle option in the Account Info section", () ->
                leadConvertPage.newExistingAccountToggle.click()
        );

        step("3. Click 'Edit' button in Opportunity Info Section, " +
                "and verify that BI picklist contains only one preselected option = " + verizonBiName, () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.businessIdentityPicklist.shouldHave(exactTextCaseSensitive(verizonBiName), ofSeconds(20));
        });

        step("4. Select 'Select an existing account' toggle option, " +
                "select New Business Account (from the 'Matched Accounts') " +
                "and click on 'Apply' button in the Account Info section", () -> {
            leadConvertPage.accountInfoEditButton.click();
            leadConvertPage.newExistingAccountToggle.click();

            leadConvertPage.selectMatchedAccount(steps.salesFlow.account.getId());
        });

        step("5. Verify that BI picklist in the Opportunity section " +
                "still contains only one preselected option = " + verizonBiName, () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();

            leadConvertPage.businessIdentityPicklist.shouldHave(exactTextCaseSensitive(verizonBiName), ofSeconds(20));
        });
    }
}
