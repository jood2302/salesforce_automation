package ngbs.quotingwizard.existingbusiness.packagetab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.components.packageselector.PackageFilter.ALL_SERVICE_OPTION;
import static com.aquiva.autotests.rc.page.components.packageselector.PackageSelector.EXPIRED_PACKAGE_BADGE_TEXT;
import static com.aquiva.autotests.rc.page.components.packageselector.PackageSelector.PACKAGE_FROM_ACCOUNT_BADGE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.CollectionCondition.*;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("P1")
@Tag("PDV")
@Tag("NGBS")
@Tag("Multiproduct-Lite")
public class PackageSelectedEqualsBillingTest extends BaseTest {
    private final Steps steps;

    private User salesRepUserWithoutPS;

    //  Test data
    private final String packageFullName;
    private final List<String> expectedListAllServices;
    private final List<String> expectedPackageFoldersListWithoutContract;
    private final List<String> expectedPackageFoldersListWithContract;

    public PackageSelectedEqualsBillingTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_NonContract_163074013.json",
                Dataset.class);

        steps = new Steps(data);

        packageFullName = data.packageFolders[0].packages[0].getFullName();

        expectedListAllServices = List.of(ALL_SERVICE_OPTION, OFFICE_SERVICE,
                RINGCENTRAL_CONTACT_CENTER_SERVICE, ENGAGE_VOICE_STANDALONE_SERVICE,
                ENGAGE_DIGITAL_STANDALONE_SERVICE, FAX_SERVICE, MEETINGS_SERVICE, RC_EVENTS_SERVICE);

        expectedPackageFoldersListWithContract = List.of(OFFICE_SERVICE,
                RINGCENTRAL_CONTACT_CENTER_SERVICE, ENGAGE_VOICE_STANDALONE_SERVICE,
                ENGAGE_DIGITAL_STANDALONE_SERVICE, MEETINGS_SERVICE, RC_EVENTS_SERVICE);

        expectedPackageFoldersListWithoutContract = List.of(OFFICE_SERVICE,
                FAX_SERVICE, MEETINGS_SERVICE);
    }

    @BeforeEach
    public void setUpTest() {
        steps.ngbs.generateBillingAccount();

        //  to be able to strictly check "Service" picklist and avoid getting Engage Legacy options
        step("Find a user with 'Sales Rep - Lightning' profile and without 'Process_Engage_Legacy' permission set", () -> {
            salesRepUserWithoutPS = getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    .withoutPermissionSet(PROCESS_ENGAGE_LEGACY_PS)
                    .execute();
        });

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUserWithoutPS);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUserWithoutPS);

        step("Login as a user with 'Sales Rep - Lightning' profile and without 'Process_Engage_Legacy' permission set", () -> {
            steps.sfdc.initLoginToSfdcAsTestUser(salesRepUserWithoutPS);
        });
    }

    @Test
    @TmsLink("CRM-8173")
    @TmsLink("CRM-21249")
    @TmsLink("CRM-26314")
    @TmsLink("CRM-28681")
    @DisplayName("CRM-8173 - Existing Business. Verify that the preselected package equals to package from Billing. \n" +
            "CRM-21249 - Existing Business | No buttons are enabled after New Quote Creation. \n" +
            "CRM-26314 - Expired Packages are not shown (Existing Business). \n" +
            "CRM-28681 - All services are available on Select Package tab for EB Office Account")
    @Description("CRM-8173 - To check that preselected package equals to Billing Info. \n" +
            "CRM-21249 - Verify that Save/Discard/Cancel Upgrade buttons behave correctly. " +
            "No buttons are enabled after New Quote Creation. \n" +
            "CRM-26314 - To check that Expired Packages are not shown in the list of available to Upgrade packages. \n" +
            "CRM-28681 - Verify that all services are available on the Select Package tab on the Quote Wizard " +
            "for EB Account, when Service (in Billing) == Office")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package from the Account, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        step("2. Open the Select Package tab and check preselected package", () -> {
            packagePage.openTab();

            packagePage.packageSelector.getSelectedPackage().getName()
                    .shouldHave(exactTextCaseSensitive(packageFullName));
            packagePage.packageSelector.getSelectedPackage().getBadge()
                    .shouldHave(exactTextCaseSensitive(PACKAGE_FROM_ACCOUNT_BADGE));
        });

        //  CRM-21249
        step("3. Check that 'Save Changes' button is disabled on the Price and Quote Details tabs, " +
                "and tab switching performs without any confirmation windows", () -> {
            cartPage.openTab();
            cartPage.saveButton.shouldBe(disabled);

            quotePage.openTab();
            quotePage.saveButton.shouldBe(disabled);
        });

        //  CRM-26314
        step("4. Return to the Select Package tab and check that Expired Packages are not shown", () -> {
            packagePage.openTab();

            packagePage.packageSelector.allPackages.shouldHave(sizeGreaterThan(0));
            //  expired packages have "Expired" badge on them
            packagePage.packageSelector.packagesBadges
                    .filterBy(exactTextCaseSensitive(EXPIRED_PACKAGE_BADGE_TEXT))
                    .shouldHave(size(0));
        });

        //  CRM-28681
        step("5. Check that Service picklist is shown with all available services for BI = 'RingCentral Inc.'", () -> {
            packagePage.packageSelector.packageFilter.servicePicklist.getOptions().shouldHave(exactTexts(expectedListAllServices));
        });

        //  CRM-28681
        step("6. Select 'All' option in Service picklist, " +
                "and check that a corresponding Package Folder with packages is shown in the Package Selector " +
                "for each Service that's available without Contract", () -> {
            packagePage.packageSelector.packageFilter.servicePicklist.selectOption(ALL_SERVICE_OPTION);

            checkPackageFoldersAndPackagesForService(expectedPackageFoldersListWithoutContract);
        });

        //  CRM-28681
        step("7. Check Contract checkbox, " +
                "and check that a corresponding Package Folder with packages is shown  " +
                "for each Service that's available with Contract", () -> {
            packagePage.packageSelector.setContractSelected(true);

            checkPackageFoldersAndPackagesForService(expectedPackageFoldersListWithContract);
        });
    }

    /**
     * Check that at least one package exists under each available package folder.
     *
     * @param expectedPackageFoldersList list of services to check whether
     *                                   package folders associated with these services are visible,
     *                                   and each package folder contains at least one package
     */
    private void checkPackageFoldersAndPackagesForService(List<String> expectedPackageFoldersList) {
        for (var packageFolderName : expectedPackageFoldersList) {
            step("Check that Package Folder is shown for '" + packageFolderName + "' Service, " +
                    "and at least one package in that package folder exists", () -> {
                var packageFolder = packagePage.packageSelector.getPackageFolderByName(packageFolderName);
                packageFolder.getSelf().shouldBe(visible);
                packageFolder.getPackagesElements().shouldHave(sizeGreaterThan(0));
            });
        }
    }
}
