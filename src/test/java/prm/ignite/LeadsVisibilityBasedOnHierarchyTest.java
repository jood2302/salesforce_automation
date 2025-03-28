package prm.ignite;

import base.BaseTest;
import com.aquiva.autotests.rc.model.prm.DealRegistrationData;
import com.aquiva.autotests.rc.model.prm.PortalUserData;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.codeborne.selenide.WebElementCondition;
import com.sforce.soap.enterprise.sobject.Deal_Registration__c;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;
import prm.PrmSteps;

import static base.Pages.*;
import static com.aquiva.autotests.rc.model.prm.DealRegistrationData.IGNITE_PARTNER_PROGRAM;
import static com.aquiva.autotests.rc.model.prm.PortalUserData.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.DqDealQualificationHelper.APPROVED_STATUS;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

@Tag("P1")
@Tag("Lead")
@Tag("Ignite")
@Tag("PRM")
public class LeadsVisibilityBasedOnHierarchyTest extends BaseTest {
    private final PrmSteps prmSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final PortalUserData partnerUserData;
    private final PortalUserData topMasterUserData;
    private final DealRegistrationData partnerDealRegTestData;
    private final DealRegistrationData topMasterDealRegTestData;

    public LeadsVisibilityBasedOnHierarchyTest() {
        prmSteps = new PrmSteps();
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        partnerUserData = prmSteps.getPortalUserData(IGNITE_PORTAL, PARTNER_HIERARCHY_LEVEL, null);
        topMasterUserData = prmSteps.getPortalUserData(IGNITE_PORTAL, IGNITE_TOP_HIERARCHY_LEVEL, null);

        partnerDealRegTestData = prmSteps.getDealRegDataForIgnitePortal(IGNITE_PARTNER_PROGRAM);
        topMasterDealRegTestData = prmSteps.getDealRegDataForIgnitePortal(IGNITE_PARTNER_PROGRAM);
    }

    @BeforeEach
    public void setUpTest() {
        step("Login as a user with Hierarchy = 'Partner Level'", () -> {
            prmSteps.initLoginToIgnitePrmPortal(partnerUserData.getUsernameSandbox(), partnerUserData.getPassword());
        });
    }

    @Test
    @TmsLink("CRM-35496")
    @DisplayName("CRM-35496 - Leads visibility based on Hierarchy")
    @Description("Verify that User with Master level can see all Leads but Users with Partner level see only their own Leads")
    public void test() {
        step("1. Choose 'Deal Registration' option from the Sales tab, and press '+ New' button", () -> {
            portalGlobalNavBar.salesButton.click();
            portalGlobalNavBar.dealRegistrationButton.click();
            dealRegistrationListPage.newButton.shouldBe(visible, ofSeconds(90)).click();
        });

        step("2. Populate all the required fields, and press the 'Submit' button, " +
                "approve the created Deal Registration via API", () -> {
            dealRegistrationCreationPage.submitFormWithPartnerProgram(partnerDealRegTestData);
            dealRegistrationRecordPage.header.shouldBe(visible, ofSeconds(30));

            updateDealRegistrationStatusToApproved(partnerDealRegTestData.lastName);
        });

        step("3. Log out from current user's session in Ignite PRM Portal, " +
                "login as User with Hierarchy = 'Ignite Top Level'", () -> {
            prmSteps.logoutViaGlobalNavbar();
            prmSteps.initLoginToIgnitePrmPortal(topMasterUserData.getUsernameSandbox(), topMasterUserData.getPassword());
        });

        step("4. Choose 'Deal Registration' option from the Sales tab, and press '+ New (On behalf Of)' button", () -> {
            portalGlobalNavBar.salesButton.click();
            portalGlobalNavBar.dealRegistrationButton.click();
            dealRegistrationListPage.newOnBehalfOfButton.shouldBe(visible, ofSeconds(90)).click();
        });

        step("5. Populate all the required fields, and press the 'Submit' button, " +
                "approve the created Deal Registration via API", () -> {
            dealRegistrationCreationPage.submitFormWithPartnerProgramAndPartnerContactSearch(topMasterDealRegTestData, topMasterUserData.contactName);

            dealRegistrationRecordPage.header.shouldBe(visible, ofSeconds(30));

            updateDealRegistrationStatusToApproved(topMasterDealRegTestData.lastName);
        });

        step("6. Choose 'Leads' option from the Sales tab, search for the Lead '" + partnerDealRegTestData.companyName + "' " +
                "and check that it is displayed, then search for the Lead '" + topMasterDealRegTestData.companyName + "' " +
                "and check that it is displayed as well", () -> {
            checkLeadsVisibilityForPortalUser(visible);
        });

        step("7. Log out from current user's session in Ignite PRM Portal, " +
                "login as User with Hierarchy = 'Partner Level'", () -> {
            prmSteps.logoutViaGlobalNavbar();
            prmSteps.initLoginToIgnitePrmPortal(partnerUserData.getUsernameSandbox(), partnerUserData.getPassword());
        });

        step("8. Choose 'Leads' option from the Sales tab, search for the Lead '" + partnerDealRegTestData.companyName + "' " +
                "and check that it is displayed, then search for the Lead '" + topMasterDealRegTestData.companyName + "' " +
                "and check that it is not displayed", () -> {
            checkLeadsVisibilityForPortalUser(hidden);
        });
    }

    /**
     * Approve the created Deal Registration via API.
     *
     * @param portalUserLastName the portal user last name to update
     */
    private void updateDealRegistrationStatusToApproved(String portalUserLastName) {
        step("Approve the created Deal Registration via API", () -> {
            var dealRegistration = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM Deal_Registration__c " +
                            "WHERE Last_Name__c = '" + portalUserLastName + "'",
                    Deal_Registration__c.class);

            dealRegistration.setStatus__c(APPROVED_STATUS);
            enterpriseConnectionUtils.update(dealRegistration);
        });
    }

    /**
     * Check visibility of the partner and master 'Deal Registration Lead'.
     *
     * @param topMasterDealRegLeadVisibility the condition of the Master's 'Deal Registration Lead' visibility to check
     */
    private void checkLeadsVisibilityForPortalUser(WebElementCondition topMasterDealRegLeadVisibility) {
        portalGlobalNavBar.salesButton.click();
        portalGlobalNavBar.leadsButton.click();
        leadsListPage.searchKeywordInput.shouldBe(visible, ofSeconds(30));

        leadsListPage.searchLeadByCompanyName(partnerDealRegTestData.companyName);
        leadsListPage.getLeadItemElement(partnerDealRegTestData.companyName).shouldBe(visible);

        leadsListPage.searchLeadByCompanyName(topMasterDealRegTestData.companyName);
        leadsListPage.getLeadItemElement(topMasterDealRegTestData.companyName).shouldBe(topMasterDealRegLeadVisibility);
    }
}
