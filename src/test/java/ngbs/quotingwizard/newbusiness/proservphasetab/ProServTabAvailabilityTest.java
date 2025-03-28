package ngbs.quotingwizard.newbusiness.proservphasetab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static base.Pages.packagePage;
import static base.Pages.wizardPage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("LTR-121")
@Tag("ProServInNGBS")
public class ProServTabAvailabilityTest extends BaseTest {
    private final Steps steps;

    private User salesRepUserWithProServInNgbsFT;

    //  Test data
    private final Map<String, Package> packageFolderNameToPackageMap;

    private final String chargeTerm;
    private final String engageDigitalServiceName;
    private final String proServServiceName;
    private final Package engageDigitalPackage;
    private final Package proServPackage;

    public ProServTabAvailabilityTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_ED_EV_CC_ProServ_Monthly_Contract.json",
                Dataset.class);
        steps = new Steps(data);

        packageFolderNameToPackageMap = Map.of(
                data.packageFolders[0].name, data.packageFolders[0].packages[0],
                data.packageFolders[3].name, data.packageFolders[3].packages[0],
                data.packageFolders[4].name, data.packageFolders[4].packages[0]
        );

        chargeTerm = data.chargeTerm;
        engageDigitalServiceName = data.packageFolders[3].name;
        proServServiceName = data.packageFolders[4].name;
        engageDigitalPackage = data.packageFolders[3].packages[0];
        proServPackage = data.packageFolders[4].packages[0];
    }

    @BeforeEach
    public void setUpTest() {
        step("Find a user with 'Sales Rep - Lightning' profile and 'ProServ in NGBS' feature toggle", () -> {
            salesRepUserWithProServInNgbsFT = getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    .withFeatureToggles(List.of(PROSERV_IN_NGBS_FT))
                    .execute();
        });

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUserWithProServInNgbsFT);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUserWithProServInNgbsFT);

        step("Log in as a user with 'Sales Rep - Lightning' profile, 'ProServ in NGBS' feature toggle, " +
                "and 'EnableSuperUserProServ In UQT' permission set", () -> {
            steps.sfdc.initLoginToSfdcAsTestUser(salesRepUserWithProServInNgbsFT);
        });
    }

    @Test
    @TmsLink("CRM-36543")
    @DisplayName("CRM-36543 - PS Phase tab availability")
    @Description("CRM-36543 - Verify that PS Phases tab is created in Quote Wizard " +
            "and the tab becomes visible only when a ProServ package is selected")
    public void test() {
        step("1. Open the Quote Wizard for the New Business Opportunity to add a new Sales Quote" +
                "and check that PS Phases tab is not displayed", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
            wizardPage.proServPhasesTabButton.shouldBe(hidden);
        });

        step("2. Select MVP, Engage Digital and ProServ packages " +
                "and check that PS Phases tab is displayed and enabled", () -> {
            steps.quoteWizard.selectPackagesForMultiProductQuote(packageFolderNameToPackageMap);
            wizardPage.proServPhasesTabButton.shouldBe(visible, enabled);
        });

        step("3. Unselect ProServ package and check that PS Phases tab is not displayed", () -> {
            packagePage.packageSelector.getPackageFolderByName(proServServiceName).expandFolder()
                    .getChildPackageByName(proServPackage.getFullName())
                    .getUnselectButton()
                    .click();
            wizardPage.proServPhasesTabButton.shouldBe(hidden);
        });

        step("4. Select ProServ package, unselect Engage Digital package" +
                "and check that PS Phases tab is displayed and enabled", () -> {
            packagePage.packageSelector.selectPackage(chargeTerm, proServServiceName, proServPackage);
            packagePage.packageSelector.getPackageFolderByName(engageDigitalServiceName).expandFolder()
                    .getChildPackageByName(engageDigitalPackage.getFullName())
                    .getUnselectButton()
                    .click();
            wizardPage.proServPhasesTabButton.shouldBe(visible, enabled);
        });
    }
}
