package prm;

import com.aquiva.autotests.rc.model.prm.DealRegistrationData;
import com.aquiva.autotests.rc.model.prm.PortalUserData;
import com.aquiva.autotests.rc.utilities.JsonUtils;

import static base.Pages.ignitePortalLoginPage;
import static base.Pages.portalGlobalNavBar;
import static com.aquiva.autotests.rc.model.prm.DealRegistrationData.RC_PRO_SERVICES_INSTALL_ISP;
import static com.aquiva.autotests.rc.model.prm.DealRegistrationData.RINGCENTRAL_BRAND;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

/**
 * Test methods for the Partner Relationship Management flows in the Salesforce Partner Portals.
 */
public class PrmSteps {

    /**
     * Get the test user's data for the specified partner portal
     * using the provided filters (e.g. hierarchy level).
     *
     * @param portal         short name of the portal (e.g. "ignite")
     * @param hierarchyLevel hierarchy level of the user (e.g. "Partner Level")
     * @param persona        persona of the user (e.g. "Partner - Full Access")
     */
    public PortalUserData getPortalUserData(String portal, String hierarchyLevel, String persona) {
        var allPortalUsers = JsonUtils.readConfigurationResourceAsList(
                "data/prm/" + portal + "_users.json",
                PortalUserData.class);

        return allPortalUsers.stream()
                .filter(user -> hierarchyLevel == null || user.hierarchy.equals(hierarchyLevel))
                .filter(user -> persona == null || user.persona.equals(persona))
                .findFirst()
                .orElseThrow();
    }

    /**
     * Get the test data for the Deal Registration form for the Ignite PRM Portal.
     *
     * @param partnerProgram partner program for the Deal Registration (e.g. "Channel Harmony", "Ignite")
     */
    public DealRegistrationData getDealRegDataForIgnitePortal(String partnerProgram) {
        var dealRegTestData = JsonUtils.readConfigurationResource(
                "data/prm/Deal_Registration.json",
                DealRegistrationData.class);

        dealRegTestData.brand = RINGCENTRAL_BRAND;
        dealRegTestData.partnerProgram = partnerProgram;
        dealRegTestData.isThisAnExistingMitelCustomer = "No";
        dealRegTestData.installationServiceProvider = RC_PRO_SERVICES_INSTALL_ISP;
        dealRegTestData.randomizeData();

        return dealRegTestData;
    }

    /**
     * Open the Ignite PRM Portal and login with the provided user's credentials.
     *
     * @param username username of the Ignite PRM test user (e.g. "mastersubfull@ringcentral.com")
     * @param password password of the Ignite PRM test user
     */
    public void initLoginToIgnitePrmPortal(String username, String password) {
        step("Open the Ignite PRM portal's login page and log in as the test user");
        ignitePortalLoginPage.openPage().login(username, password);
        portalGlobalNavBar.homeButton.shouldBe(visible, ofSeconds(30));
    }

    /**
     * Log out from current user's session in the Ignite PRM Portal
     * via Global Navigation bar.
     */
    public void logoutViaGlobalNavbar() {
        portalGlobalNavBar.profileMenu.click();
        portalGlobalNavBar.logOutButton.click();

        ignitePortalLoginPage.waitUntilLoaded();
    }
}
