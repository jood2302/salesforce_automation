package prm.ignite;

import base.BaseTest;
import com.aquiva.autotests.rc.model.prm.PortalUserData;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import prm.PrmSteps;

import static base.Pages.dealRegistrationListPage;
import static com.aquiva.autotests.rc.model.prm.PortalUserData.*;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("DealRegistration")
@Tag("PRM")
public class DealRegistrationNewButtonAvailabilityTest extends BaseTest {
    private final PrmSteps prmSteps;

    //  Test data
    private final PortalUserData topMasterUserData;
    private final PortalUserData partnerFullUserData;
    private final PortalUserData partnerLimitedUserData;

    public DealRegistrationNewButtonAvailabilityTest() {
        prmSteps = new PrmSteps();

        topMasterUserData = prmSteps.getPortalUserData(IGNITE_PORTAL, IGNITE_TOP_HIERARCHY_LEVEL, null);
        partnerFullUserData = prmSteps.getPortalUserData(IGNITE_PORTAL, PARTNER_HIERARCHY_LEVEL, PARTNER_FULL_ACCESS_PERSONA);
        partnerLimitedUserData = prmSteps.getPortalUserData(IGNITE_PORTAL, PARTNER_HIERARCHY_LEVEL, PARTNER_LIMITED_ACCESS_PERSONA);
    }

    @Test
    @Tag("KnownIssue")
    @Issue("PBC-25899")
    @TmsLink("CRM-35516")
    @TmsLink("CRM-35517")
    @DisplayName("CRM-35516 - '+New' button availability in the Registered Deals list (Ignite). \n" +
            "CRM-35517 - '+New(On behalf Of)' button availability in the Registered Deals list (Ignite)")
    @Description("CRM-35516 - Verify that the '+ New' button is available in Registered Deals list in PRM for any Partner User " +
            "below the Top Master User in the hierarchy and not available for Top Master User. \n" +
            "CRM-35517 - Verify that the '+ New(On behalf Of)' button is available in Registered Deals list for any Partner User " +
            "above the Partner Limited Access User in the hierarchy and not available for Partner Limited Access User")
    public void test() {
        step("1. Open the PRM Portal and log in as a user with 'Partner Level' hierarchy with 'Partner - Full Access' persona", () ->
                prmSteps.initLoginToIgnitePrmPortal(partnerFullUserData.getUsernameSandbox(), partnerFullUserData.getPassword())
        );

        step("2. Open the 'Registered Deals' page, " +
                "and check that '+ New' and '+ New(On behalf Of)' buttons are displayed in the header of the list", () -> {
            dealRegistrationListPage.openPage();
            //  CRM-35516
            dealRegistrationListPage.newButton.shouldBe(visible);
            //  CRM-35517
            dealRegistrationListPage.newOnBehalfOfButton.shouldBe(visible);
        });

        step("3. Log out from current user's session in PRM Portal", () -> {
            prmSteps.logoutViaGlobalNavbar();
        });

        step("4. Open the PRM Portal and log in as user with 'Ignite Top Level' hierarchy", () -> {
            prmSteps.initLoginToIgnitePrmPortal(topMasterUserData.getUsernameSandbox(), topMasterUserData.getPassword());
        });

        step("5. Open the 'Registered Deals' page, " +
                "and check that '+ New' button is hidden, " +
                "and '+ New(On behalf Of)' button is displayed in the header of the list", () -> {
            dealRegistrationListPage.openPage();
            //  CRM-35517
            dealRegistrationListPage.newOnBehalfOfButton.shouldBe(visible);
            //  CRM-35516
            //  TODO Known Issue PBC-25899 ('+ New' button is displayed for Top Master User, but it should not be)
            dealRegistrationListPage.newButton.shouldBe(hidden);
        });

        step("6. Log out from current user's session in PRM Portal", () -> {
            prmSteps.logoutViaGlobalNavbar();
        });

        step("7. Open the PRM Portal and log in as a user with 'Partner Level' hierarchy with 'Partner - Limited Access' persona", () -> {
            prmSteps.initLoginToIgnitePrmPortal(partnerLimitedUserData.getUsernameSandbox(), partnerLimitedUserData.getPassword());
        });

        step("8. Open the 'Registered Deals' page, " +
                "and check that '+ New' button is displayed, " +
                "and '+ New(On behalf Of)' button is hidden in the header of the list", () -> {
            dealRegistrationListPage.openPage();
            //  CRM-35516
            dealRegistrationListPage.newButton.shouldBe(visible);
            //  CRM-35517
            dealRegistrationListPage.newOnBehalfOfButton.shouldBe(hidden);
        });
    }
}
