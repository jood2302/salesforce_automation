package ngbs.quotingwizard.newbusiness.packagetab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Opportunity;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import java.util.Map;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.components.packageselector.Package.PACKAGE_INFO_ATTRIBUTE;
import static com.aquiva.autotests.rc.page.components.packageselector.PackageFilter.ALL_SERVICE_OPTION;
import static com.aquiva.autotests.rc.page.components.packageselector.PackageFolder.beExpanded;
import static com.aquiva.autotests.rc.page.components.packageselector.PackageSelector.EXPIRED_PACKAGE_BADGE_TEXT;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteLineItemHelper.ANNUAL_CHARGE_TERM;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteLineItemHelper.MONTHLY_CHARGE_TERM;
import static com.codeborne.selenide.CollectionCondition.*;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

@Tag("P0")
@Tag("P1")
@Tag("NGBS")
@Tag("PackageTab")
@Tag("Multiproduct-Lite")
public class CheckPackagesTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final PackageFolder[] officeMeetingsFaxPackageFolders;
    private final PackageFolder[] ccAndEngagePackageFolders;
    private final Map<String, Package> packageFolderNameAndPackageMapWithEdPackage;
    private final Map<String, Package> packageFolderNameAndPackageMapWithEvPackage;
    private final Package rcCcPackage;
    private final Package engageVoicePackage;
    private final Package engageDigitalPackage;
    private final String rcCcPackageFolder;
    private final String engageVoicePackageFolder;
    private final String engageDigitalPackageFolder;
    private final PackageFolder packageFolderToSelect;
    private final Package packageToSelect;
    private final String engageService;
    private final String meetingsService;
    private final String notSelectedPackageName;
    private final String packageVersionRegex;
    private final String priceValueRegex;
    private final String monthlyChargeTerm;
    private final String annualChargeTerm;

    public CheckPackagesTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_AllPackageFolders_Annual_Contract_NoProducts.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        officeMeetingsFaxPackageFolders = new PackageFolder[]{
                data.packageFolders[0],
                data.packageFolders[4],
                data.packageFolders[5]
        };
        ccAndEngagePackageFolders = new PackageFolder[]{
                data.packageFolders[1],
                data.packageFolders[2],
                data.packageFolders[3]
        };
        packageFolderNameAndPackageMapWithEdPackage = Map.of(
                data.packageFolders[0].name, data.packageFolders[0].packages[0],
                data.packageFolders[1].name, data.packageFolders[1].packages[0],
                data.packageFolders[3].name, data.packageFolders[3].packages[0]
        );
        packageFolderNameAndPackageMapWithEvPackage = Map.of(
                data.packageFolders[0].name, data.packageFolders[0].packages[0],
                data.packageFolders[2].name, data.packageFolders[2].packages[0]
        );

        rcCcPackage = data.packageFolders[1].packages[0];
        engageVoicePackage = data.packageFolders[2].packages[0];
        engageDigitalPackage = data.packageFolders[3].packages[0];
        rcCcPackageFolder = data.packageFolders[1].name;
        engageVoicePackageFolder = data.packageFolders[2].name;
        engageDigitalPackageFolder = data.packageFolders[3].name;
        packageFolderToSelect = officeMeetingsFaxPackageFolders[0];
        packageToSelect = packageFolderToSelect.packages[0];
        engageService = ccAndEngagePackageFolders[1].name;
        meetingsService = officeMeetingsFaxPackageFolders[1].name;
        notSelectedPackageName = packageFolderToSelect.packages[1].getFullName();

        //  e.g. "Version - 1", etc.
        packageVersionRegex = "Version\\s-\\s\\d";
        //  e.g. "30.99", "525.00", etc.
        priceValueRegex = "^\\d+\\.\\d{2}$";

        monthlyChargeTerm = "Monthly";
        annualChargeTerm = "Annual";
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);

        //  workaround to be able to view all the package folders in the Quote Wizard
        step("Set Opportunity.Tier_Name__c = null via API", () -> {
            var opportunityToUpdate = new Opportunity();
            opportunityToUpdate.setId(steps.quoteWizard.opportunity.getId());
            opportunityToUpdate.setFieldsToNull(new String[]{"Tier_Name__c"});
            enterpriseConnectionUtils.update(opportunityToUpdate);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-5444")
    @TmsLink("CRM-5445")
    @TmsLink("CRM-1884")
    @TmsLink("CRM-11302")
    @TmsLink("CRM-9338")
    @TmsLink("CRM-21371")
    @TmsLink("CRM-11367")
    @TmsLink("CRM-26997")
    @TmsLink("CRM-28330")
    @TmsLink("CRM-28319")
    @DisplayName("CRM-5444 - New Business. Annual charge term. Verify that the packages are displayed. \n" +
            "CRM-5445 - New Business. Monthly charge term. Verify that the packages are displayed. \n" +
            "CRM-1884 - User is able to select package Annual Charge Term. \n" +
            "CRM-11302 - User can choose a Package and a Plan. New Business. \n" +
            "CRM-9338 - POC Packages are not shown on Sales Quotes. \n" +
            "CRM-21371 - New Business | No buttons are enabled after New Package Selection. \n" +
            "CRM-11367 - Expired Packages are not shown (New Business). \n" +
            "CRM-26997 - Redesign QT 2.0. Package Selection. Package Component (Design). \n" +
            "CRM-28330 - Contract is mandatory for EV, ED, CC. \n" +
            "CRM-28319 - Office, Engage and RCCC packages can be selected together. New business")
    @Description("CRM-5444 - To check that packages are present in package folders. \n" +
            "CRM-5445 - Verify that contracts are available for annual/monthly charge terms. \n" +
            "CRM-1884 - Check that on the 'Package' Tab available packages can be selected. \n" +
            "CRM-11302 - To check that user can choose a Package and a Plan for the New Business Opportunity. \n" +
            "CRM-9338 - Verify that POC Packages are not shown on Sales Quotes on the Select Package tab in Quoting Wizard. \n" +
            "CRM-21371 - Verify that Save/Discard buttons behave correctly. \n" +
            "CRM-11367 - To check that Expired Packages are not shown in the list of available packages. \n" +
            "CRM-26997 - Verify redesign Package Selection and Package Component in QT 2.0 (design). \n" +
            "CRM-28330 - Verify that user cannot select Engage Voice / Engage Digital / Contact Center packages " +
            "without Contract on Multiproduct Quote. \n" +
            "CRM-28319 - Verify that: \n" +
            " - Office, Engage Digital Standalone and Contact Center packages " +
            "are compatible with each other (can be selected together in Multi-Product) \n" +
            " - both RingCX and Engage Digital Standalone can't be selected together \n" +
            " - both Contact Center and RingCX can't be selected together")
    public void test() {
        step("1. Open the Quote Wizard for the New Business Opportunity to add a new Sales Quote", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
            packagePage.packageSelector.packageFilter.servicePicklist.selectOption(ALL_SERVICE_OPTION);
            //  to be able to see packages that do not have contract option
            packagePage.packageSelector.setContractSelected(false);
        });

        //  CRM-11367
        step("2. Check that Expired Packages are not shown on the Select Package tab", () -> {
            //  expired packages have "Expired" badge on them
            packagePage.packageSelector.packagesBadges
                    .filterBy(exactTextCaseSensitive(EXPIRED_PACKAGE_BADGE_TEXT))
                    .shouldHave(size(0));
        });

        //  CRM-28319
        step("3. Check that Engage and Contact Center packages are not shown on the Select Package tab", () -> {
            for (var packageFolder : ccAndEngagePackageFolders) {
                step("Check that the package folder for service = '" + packageFolder + "' is not shown", () -> {
                    packagePage.packageSelector.getPackageFolderByName(packageFolder.name).getSelf().shouldBe(hidden);
                });
            }
        });

        //  CRM-9338
        step("4. Check that POC Packages are not shown", () -> {
            packagePage.packageSelector.allPackages
                    .should(allMatch("All packages should contain attribute '" + PACKAGE_INFO_ATTRIBUTE + "'",
                            pkg -> pkg.getAttribute(PACKAGE_INFO_ATTRIBUTE) != null));

            packagePage.packageSelector.allPackages
                    .filterBy(attributeMatching(PACKAGE_INFO_ATTRIBUTE, "^POC_.+_.+$"))
                    .shouldHave(size(0));
        });

        //  CRM-5445
        step("5. Select 'Monthly' charge term, and check available packages", () ->
                checkPackagesInFolder(officeMeetingsFaxPackageFolders, MONTHLY_CHARGE_TERM)
        );

        //  CRM-5444
        step("6. Select 'Annual' charge term, and check available packages", () ->
                checkPackagesInFolder(officeMeetingsFaxPackageFolders, ANNUAL_CHARGE_TERM)
        );

        //  CRM-1884 and CRM-11302
        step("7. Select '" + packageToSelect.name + "' package with 'Annual' charge term, " +
                "create a Quote and check the selected package and service", () -> {
            packagePage.packageSelector.selectPackage(data.chargeTerm, packageFolderToSelect.name, packageToSelect);
            packagePage.saveChanges();

            packagePage.packageSelector.getSelectedPackage().getName()
                    .shouldHave(exactTextCaseSensitive(packageToSelect.getFullName()));
        });

        //  CRM-28330, CRM-28319
        step("8. Select 'All' option in Service selector and check that Engage and Contact Center packages are not shown", () -> {
            packagePage.packageSelector.packageFilter.servicePicklist.selectOption(ALL_SERVICE_OPTION);

            for (var packageFolder : ccAndEngagePackageFolders) {
                step("Check that the package folder for service = '" + packageFolder + "' is not shown", () -> {
                    packagePage.packageSelector.getPackageFolderByName(packageFolder.name).getSelf().shouldBe(hidden);
                });
            }
        });

        //  CRM-28319
        step("9. Check Contract checkbox, check that Engage and Contact Center packages are visible", () -> {
            packagePage.packageSelector.setContractSelected(true);

            for (var packageFolder : ccAndEngagePackageFolders) {
                step("Check that the package folder for service = '" + packageFolder.name + "' is shown", () -> {
                    packagePage.packageSelector.getPackageFolderByName(packageFolder.name).getSelf().shouldBe(visible);
                });
            }
        });

        //  CRM-28330
        step("10. Select Engage package, set Service = '" + engageService + "', " +
                "and check that Engage package folder is expanded, and other service sections are collapsed", () -> {
            packagePage.packageSelector.selectPackage(data.chargeTerm, engageService, engageVoicePackage);

            packagePage.packageSelector.packageFilter.servicePicklist.selectOption(engageService);

            packagePage.packageSelector.getPackageFolderByName(engageService).getSelf().should(beExpanded);
            packagePage.packageSelector.packageFolders
                    .exclude(matchText(".*" + engageService + ".*"))
                    .asDynamicIterable()
                    .forEach(otherPackageFolder -> otherPackageFolder.shouldNot(beExpanded));
        });

        //  CRM-28330
        step("11. Expand 'Meetings' service section and check that its elements are disabled", () -> {
            var meetingsPackageFolder = packagePage.packageSelector.getPackageFolderByName(meetingsService).expandFolder();

            meetingsPackageFolder.getExpandButton().shouldHave(cssClass("disabled"));
            meetingsPackageFolder.getNumberOfLicensesInput().shouldBe(disabled);
            meetingsPackageFolder.getPackagesElements()
                    .asDynamicIterable()
                    .forEach(meetingsPackage -> meetingsPackage.shouldHave(cssClass("disabled")));

            meetingsPackageFolder.getAllPackages().forEach(pkg -> pkg.getSelectButton().shouldBe(disabled));
        });

        //  CRM-28330
        step("12. Check that 'Save and Continue' button is enabled", () -> {
            packagePage.saveAndContinueButton.shouldBe(enabled);
        });

        //  CRM-28330
        step("13. Select 'All' option in Service selector, uncheck Contract checkbox " +
                "and check that selected Engage Package is not shown", () -> {
            packagePage.packageSelector.packageFilter.servicePicklist.selectOption(ALL_SERVICE_OPTION);
            packagePage.packageSelector.setContractSelected(false);

            packagePage.packageSelector.getPackageFolderByName(engageService).getSelf().shouldBe(hidden);
        });

        //  CRM-28330
        step("14. Check that Engage Voice, Engage Digital, RingCentral Contact Center services sections are not displayed " +
                "and 'Save and Continue' button is inactive", () -> {
            for (var packageFolder : ccAndEngagePackageFolders) {
                packagePage.packageSelector.getPackageFolderByName(packageFolder.name).getSelf().shouldBe(hidden);
            }

            packagePage.saveAndContinueButton.shouldBe(disabled);
        });

        //  CRM-26997
        step("15. Check that all Packages contain Package names, versions and prices with currency ISO codes", () -> {
            packagePage.packageSelector.packagesNames.shouldHave(sizeGreaterThan(0));

            packagePage.packageSelector.packagesNames.asDynamicIterable()
                    .forEach(packageName -> packageName.shouldBe(visible));
            packagePage.packageSelector.packagesNames.asDynamicIterable()
                    .forEach(packageName -> packageName.shouldNotBe(empty));
            packagePage.packageSelector.packagesVersions.asDynamicIterable()
                    .forEach(packageVersion -> packageVersion.should(matchText(packageVersionRegex)));
            packagePage.packageSelector.packagesPriceValues.asDynamicIterable()
                    .forEach(packagePriceValue -> packagePriceValue.should(matchText(priceValueRegex)));
            packagePage.packageSelector.packagesPriceCurrencies.asDynamicIterable()
                    .forEach(packagePriceCurrency -> packagePriceCurrency.shouldHave(exactText(data.getCurrencyIsoCode())));
        });

        //  CRM-26997
        step("16. Check that selected Package has 'Unselect' button", () -> {
            packagePage.packageSelector.getSelectedPackage().getUnselectButton().shouldBe(visible, enabled);
        });

        //  CRM-26997
        step("17. Check that the Package that's not selected has 'Select' button", () -> {
            packagePage.packageSelector.getPackageFolderByName(packageFolderToSelect.name)
                    .getChildPackageByName(notSelectedPackageName)
                    .getSelectButton()
                    .shouldBe(visible, enabled);
        });

        //  CRM-26997
        step("18. Check Monthly and Annual buttons in Payment Plan", () -> {
            packagePage.packageSelector.packageFilter.getChargeTermInput(monthlyChargeTerm).shouldBe(enabled);
            packagePage.packageSelector.packageFilter.getChargeTermInput(annualChargeTerm).shouldBe(enabled);
        });

        //  CRM-21371 and CRM-26997
        step("19. Check that 'Save and Continue' button is disabled on the Select Package tab after package selection," +
                "'Save Changes' buttons on the Price and Quote Details tabs are disabled too, " +
                "and tab switching performs without any confirmation windows", () -> {
            packagePage.saveAndContinueButton.shouldBe(disabled);

            //  confirmation window visibility check is inside openTab() methods
            cartPage.openTab();
            cartPage.saveButton.shouldBe(disabled);

            quotePage.openTab();
            quotePage.saveButton.shouldBe(disabled);
        });

        //  CRM-28319
        step("20. Open the Select Package tab, set the 'Contract' checkbox as selected, " +
                "select Office, ED and RC CC packages one for each service, check that all packages are selected " +
                "and all Engage Voice packages are unable to select", () -> {
            packagePage.openTab();
            packagePage.packageSelector.setContractSelected(true);
            packageFolderNameAndPackageMapWithEdPackage.forEach((packageFolderName, aPackage) ->
                    step("Select the package '" + aPackage.getFullName() + "' for the service = " + packageFolderName, () ->
                            packagePage.packageSelector
                                    .getPackageFolderByName(packageFolderName)
                                    .getPackageByDataAttribute(aPackage.id, aPackage.version, aPackage.getPackageType())
                                    .selectPackage()
                    ));

            packageFolderNameAndPackageMapWithEdPackage.forEach((packageFolderName, aPackage) ->
                    step("Check that package '" + aPackage.getFullName() + "' is selected", () -> {
                        packagePage.packageSelector
                                .getPackageFolderByName(packageFolderName)
                                .getChildPackageByName(aPackage.getFullName())
                                .getUnselectButton()
                                .shouldBe(visible);
                    }));

            packagePage.packageSelector
                    .getPackageFolderByName(engageVoicePackageFolder)
                    .getAllPackages()
                    .forEach(pkg -> pkg.getSelectButton().shouldBe(disabled));
        });

        //  CRM-28319
        step("21. Unselect Engage Digital package, check that all Engage Voice packages are unable to be selected, " +
                "unselect RC CC package and check that all Engage Voice packages are available to be selected", () -> {
            packagePage.packageSelector.unselectPackage(engageDigitalPackageFolder, engageDigitalPackage);
            packagePage.packageSelector
                    .getPackageFolderByName(engageVoicePackageFolder)
                    .getAllPackages()
                    .forEach(pkg -> pkg.getSelectButton().shouldBe(disabled));

            packagePage.packageSelector.unselectPackage(rcCcPackageFolder, rcCcPackage);
            packagePage.packageSelector
                    .getPackageFolderByName(engageVoicePackageFolder)
                    .getAllPackages()
                    .forEach(pkg -> pkg.getSelectButton().shouldBe(enabled));
        });

        //  CRM-28319
        step("22. Select Engage Voice package, check that Office and Engage Voice package are selected " +
                "and check that RC CC and Engage Digital packages are unavailable to be selected", () -> {
            packagePage.packageSelector.selectPackage(data.chargeTerm, engageVoicePackageFolder, engageVoicePackage);
            packageFolderNameAndPackageMapWithEvPackage.forEach((packageFolderName, aPackage) ->
                    step("Check that package '" + aPackage.getFullName() + "' is selected", () -> {
                        packagePage.packageSelector
                                .getPackageFolderByName(packageFolderName)
                                .getChildPackageByName(aPackage.getFullName())
                                .getUnselectButton()
                                .shouldBe(visible);
                    }));

            packagePage.packageSelector
                    .getPackageFolderByName(rcCcPackageFolder)
                    .getAllPackages()
                    .forEach(pkg -> pkg.getSelectButton().shouldBe(disabled));
            packagePage.packageSelector
                    .getPackageFolderByName(engageDigitalPackageFolder)
                    .getAllPackages()
                    .forEach(pkg -> pkg.getSelectButton().shouldBe(disabled));
        });
    }

    /**
     * Select a charge term and check all the available packages in the package folders from the test data.
     *
     * @param packageFolders package folders to check ("Office", "Fax", etc...)
     * @param chargeTerm     charge term to select ("Monthly" or "Annual")
     */
    @Step("Check packages in the selected package folder for the chosen charge term against the provided test data")
    private void checkPackagesInFolder(PackageFolder[] packageFolders, String chargeTerm) {
        packagePage.packageSelector.packageFilter.selectChargeTerm(chargeTerm);

        for (var currentPackageFolder : packageFolders) {
            step("Check packages for a package folder '" + currentPackageFolder.name + "'", () -> {
                packagePage.packageSelector.getPackageFolderByName(currentPackageFolder.name).expandFolder();

                var actualPackagesElements = packagePage.packageSelector
                        .getPackageFolderByName(currentPackageFolder.name)
                        .getPackageFullNameElements();

                var expectedPackageNames = stream(currentPackageFolder.packages)
                        .map(Package::getFullName)
                        .collect(toList());

                actualPackagesElements.should(containExactTextsCaseSensitive(expectedPackageNames));
            });
        }
    }
}
