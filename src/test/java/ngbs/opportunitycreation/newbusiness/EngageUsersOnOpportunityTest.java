package ngbs.opportunitycreation.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Opportunity;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.opportunityCreationPage;
import static base.Pages.opportunityPage;
import static com.aquiva.autotests.rc.page.opportunity.OpportunityCreationPage.NEW_NUMBER_OF_USERS;
import static com.aquiva.autotests.rc.utilities.NumberHelper.doubleToIntToString;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.CollectionCondition.itemWithText;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Tag("P1")
@Tag("Engage")
@Tag("QOP")
public class EngageUsersOnOpportunityTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User ddUserWithEngageLegacyPermissionSet;

    //  Test data
    private final String engageVoiceStandaloneService;
    private final String engageDigitalLegacyService;
    private final String newNumberOfUsers;
    private final String newNumberOfUsersInitial;

    public EngageUsersOnOpportunityTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/opportunitycreation/newbusiness/RC_EVStandaloneAndEDLegacy_Monthly_Contract_QOP.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        engageVoiceStandaloneService = data.packageFolders[0].name;
        engageDigitalLegacyService = data.packageFolders[1].name;
        newNumberOfUsers = String.valueOf(data.engageUsers);
        newNumberOfUsersInitial = "1";
    }

    @BeforeEach
    public void setUpTest() {
        step("Find a user with 'Deal Desk Lightning' profile and 'Process_Engage_Legacy' permission set", () -> {
            ddUserWithEngageLegacyPermissionSet = getUser()
                    .withProfile(DEAL_DESK_LIGHTNING_PROFILE)
                    .withPermissionSet(PROCESS_ENGAGE_LEGACY_PS)
                    .execute();
        });

        steps.salesFlow.createAccountWithContactAndContactRole(ddUserWithEngageLegacyPermissionSet);

        step("Login as a user with 'Deal Desk Lightning' profile and 'Process_Engage_Legacy' permission set", () -> {
            steps.sfdc.initLoginToSfdcAsTestUser(ddUserWithEngageLegacyPermissionSet);
        });
    }

    @Test
    @TmsLink("CRM-20399")
    @DisplayName("CRM-20399 - 'New Number of Users' - New Business")
    @Description("Verify that the new field 'New Number of Users' populates a field on Opportunity depending on the selected Package: \n\n" +
            "- Engage Digital Standalone/Legacy - 'Forecast Engage DIGITAL Users' field on Opportunity \n" +
            "- Engage Voice Standalone/Legacy - 'Forecast Engage VOICE Users' field on Opportunity")
    public void test() {
        step("1. Check 'New Number of Users' field on QOP for Engage Account for " + engageVoiceStandaloneService + ", " +
                "create Opportunity and check that 'Forecast Engage Voice Users' = " + newNumberOfUsers, () ->
                createEngageOpportunityTestSteps(engageVoiceStandaloneService, "0", newNumberOfUsers)
        );

        step("2. Check 'New Number of Users' field on QOP for Engage Account for " + engageDigitalLegacyService + ", " +
                "create Opportunity and check that 'Forecast Engage Digital Users' = " + newNumberOfUsers, () ->
                createEngageOpportunityTestSteps(engageDigitalLegacyService, newNumberOfUsers, "0")
        );
    }

    /**
     * <p> 1. Open QOP for Account, populate all the mandatory fields. </p>
     * <p> 2. Select Business Identity, an Engage service and check 'New Number of Users' field. </p>
     * <p> 3. Press 'Continue to Opportunity'. </p>
     * <p> 4. Check 'Forecasted Engage Voice Users' and 'Forecasted Engage Digital Users' fields
     * on the created Opportunity. </p>
     *
     * @param engageService              Engage Service for the package selection (e.g. "Engage Voice Standalone")
     * @param expectedEngageDigitalUsers expected number of 'Forecasted Engage Digital Users' in the Opportunity
     * @param expectedEngageVoiceUsers   expected number of 'Forecasted Engage Voice Users' in the Opportunity
     */
    private void createEngageOpportunityTestSteps(String engageService,
                                                  String expectedEngageDigitalUsers, String expectedEngageVoiceUsers) {
        step("Open QOP for Engage Account, populate Close Date and Provisioning Details", () -> {
            opportunityCreationPage.openPage(steps.salesFlow.account.getId());

            opportunityCreationPage.populateCloseDate();
            opportunityCreationPage.provisioningDetailsTextArea.setValue(TEST_STRING);
        });

        step("Select '" + engageService + "' service, and check 'New Number of Users' field", () -> {
            opportunityCreationPage.servicePicklist.getOptions().shouldHave(itemWithText(engageService), ofSeconds(10));
            opportunityCreationPage.servicePicklist.selectOption(engageService);
            opportunityCreationPage.newNumberOfDLsLabel.shouldHave(text(NEW_NUMBER_OF_USERS));
            opportunityCreationPage.newNumberOfDLsInput.shouldHave(value(newNumberOfUsersInitial));
        });

        step("Set 'New Number of Users' = " + newNumberOfUsers + " and click 'Continue to Opportunity", () -> {
            opportunityCreationPage.newNumberOfDLsInput.setValue(newNumberOfUsers);
            steps.opportunityCreation.pressContinueToOpp();
        });

        step("Check the number of 'Forecasted Engage Voice Users' and 'Forecasted Engage Digital Users' " +
                "on the created Opportunity", () -> {
            var engageOpportunity = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, ForcastedDimeloUsers__c, Forecast_Connect_First_Users__c " +
                            "FROM Opportunity " +
                            "WHERE Id = '" + opportunityPage.getCurrentRecordId() + "'",
                    Opportunity.class);
            assertThat(doubleToIntToString(engageOpportunity.getForcastedDimeloUsers__c()))
                    .as("Opportunity.ForcastedDimeloUsers__c value (Engage Digital Users)")
                    .isEqualTo(expectedEngageDigitalUsers);
            assertThat(doubleToIntToString(engageOpportunity.getForecast_Connect_First_Users__c()))
                    .as("Opportunity.Forecast_Connect_First_Users__c value (Engage Voice Users)")
                    .isEqualTo(expectedEngageVoiceUsers);
        });
    }
}
