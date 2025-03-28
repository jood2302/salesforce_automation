package ngbs.opportunitycreation.newbusiness;

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
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.hidden;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("QOP")
@Tag("Verizon")
public class VerizonOpportunityCreationTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User salesUserWithVerizonPermissionSet;

    //  Test data
    private final String officeService;

    public VerizonOpportunityCreationTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/opportunitycreation/newbusiness/RC_Verizon_Monthly_NonContract_QOP.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        officeService = data.packageFolders[0].name;
    }

    @BeforeEach
    public void setUpTest() {
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
            steps.salesFlow.account.setRC_Brand__c(data.brandName);
            enterpriseConnectionUtils.update(steps.salesFlow.account);
        });

        step("Login as a user with 'Sales Rep - Lightning' profile and 'RingCentral_with_Verizon' permission set", () -> {
            steps.sfdc.initLoginToSfdcAsTestUser(salesUserWithVerizonPermissionSet);
        });
    }

    @Test
    @TmsLink("CRM-26114")
    @DisplayName("CRM-26114 - QOP for Verizon (New Business) with specified Brand on Account")
    @Description("Verify that Opportunity can be created for the 'RingCentral with Verizon' Brand")
    public void test() {
        step("1. Open QOP for the test Account", () ->
                opportunityCreationPage.openPage(steps.salesFlow.account.getId())
        );

        step("2. Check that 'RingCentral with Verizon' Business Identity is preselected, then populate Close Date field", () -> {
            opportunityCreationPage.businessIdentityPicklist.getSelectedOption()
                    .shouldHave(exactTextCaseSensitive(data.getBusinessIdentityName()));
            opportunityCreationPage.populateCloseDate();
        });

        step("3. Check that Service picklist contains only 'Office' option and 'New Number of DLs' field is hidden", () -> {
            opportunityCreationPage.servicePicklist.getOptions().shouldHave(exactTextsCaseSensitiveInAnyOrder(officeService));

            opportunityCreationPage.newNumberOfDLsInput.shouldBe(hidden);
        });

        step("4. Click 'Continue to Opportunity' button, " +
                "and check that the created Opportunity's page is opened", () ->
                steps.opportunityCreation.pressContinueToOpp()
        );
    }
}
