package ngbs.quotingwizard.newbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Package_Main_Licenses__c;
import com.sforce.soap.enterprise.sobject.QuoteLineItem;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.getStringListAsString;
import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("PackageTab")
@Tag("Multiproduct-Lite")
public class PackageMainLicensesMultiProductTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private List<String> allMainLicenseDataIds;

    //  Test data
    private final String rcCcPackageFolder;
    private final Package rcCcPackage;
    private final String engageDigitalPackageFolder;
    private final Package engageDigitalPackage;
    private final String engageVoicePackageFolder;
    private final Package engageVoicePackage;
    private final Map<String, Package> packageFolderNameToPackageMap;

    private final Product dlUnlimited;
    private final Product rcCcGuideProduct;
    private final Product engageVoiceBundleProduct;
    private final Product engageDigitalSeatProduct;

    private final List<String> officeSubgroups;
    private final List<String> rcContactCenterSubgroups;
    private final List<String> engageVoiceSubgroups;
    private final List<String> engageDigitalSubgroups;

    public PackageMainLicensesMultiProductTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_ED_EV_CC_ProServ_Monthly_Contract.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        rcCcPackageFolder = data.packageFolders[1].name;
        rcCcPackage = data.packageFolders[1].packages[0];
        engageVoicePackageFolder = data.packageFolders[2].name;
        engageVoicePackage = data.packageFolders[2].packages[0];
        engageDigitalPackageFolder = data.packageFolders[3].name;
        engageDigitalPackage = data.packageFolders[3].packages[0];
        var officePackageFolder = data.packageFolders[0].name;
        var officePackage = data.packageFolders[0].packages[0];
        packageFolderNameToPackageMap = Map.of(
                officePackageFolder, officePackage,
                rcCcPackageFolder, rcCcPackage,
                engageDigitalPackageFolder, engageDigitalPackage
        );

        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        rcCcGuideProduct = rcCcPackage.productsOther[0];
        engageVoiceBundleProduct = engageVoicePackage.productsOther[0];
        engageDigitalSeatProduct = engageDigitalPackage.productsDefault[0];

        officeSubgroups = List.of("Main", "Additional DLs", "Specialty Analog Services");
        rcContactCenterSubgroups = List.of("Acqueon", "Attendant", "Expert", "Gryphon", "Guide", "iBenchmark", "LanguageLine",
                "Lucency", "SpinSci", "Sycurio Voice");
        engageVoiceSubgroups = List.of("IVR", "Outbound", "Toll Free");
        engageDigitalSubgroups = List.of("On Demand", "Purchase");
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-30294")
    @TmsLink("CRM-28757")
    @DisplayName("CRM-30294 - 'Package Main Licenses' Setting determines which licenses will be automatically added for all services. \n" +
            "CRM-28757 - Licenses are displayed in correct subgroups of all selected services")
    @Description("CRM-30294 - Verify that while creating Multi-Product Quote licenses that are specified in the settings " +
            "will be added automatically in Quote. \n" +
            "CRM-28757 - Verify that licenses from all selected Services are displayed in correct subgroups on the Add Products Tab")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "and select MVP, RC Contact Center, and Engage Digital packages for it", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.selectPackagesForMultiProductQuote(packageFolderNameToPackageMap);
        });

        //  CRM-28757
        step("2. Open the Add Products tab, " +
                "and check that correct subgroups are shown for the given groups for all the selected services, " +
                "and that correct licenses are shown in those subgroups", () -> {
            productsPage.openTab();

            checkProductAndSubgroups(dlUnlimited, officeSubgroups);
            checkProductAndSubgroups(rcCcGuideProduct, rcContactCenterSubgroups);
            checkProductAndSubgroups(engageDigitalSeatProduct, engageDigitalSubgroups);
        });

        //  CRM-28757
        step("3. Open the Select Package tab, " +
                "unselect the Engage Digital and RingCentral Contact Center packages, select Engage Voice package, " +
                "open the Add Products tab and check that correct subgroups are shown for the given groups for the Engage Voice service " +
                "and that correct licenses are shown in those subgroups", () -> {
            packagePage.openTab();

            packagePage.packageSelector.unselectPackage(engageDigitalPackageFolder, engageDigitalPackage);
            packagePage.packageSelector.unselectPackage(rcCcPackageFolder, rcCcPackage);
            packagePage.packageSelector.selectPackage(data.chargeTerm, engageVoicePackageFolder, engageVoicePackage);

            productsPage.openTab();
            checkProductAndSubgroups(engageVoiceBundleProduct, engageVoiceSubgroups);
        });

        //  CRM-30294
        step("3. Open the Select Package tab, " +
                "unselect the Engage Voice package, select Engage Digital and RingCentral Contact Center packages, " +
                "open the Price tab, check that Main licenses are automatically added for all selected packages " +
                "and save changes", () -> {
            packagePage.openTab();

            packagePage.packageSelector.unselectPackage(engageVoicePackageFolder, engageVoicePackage);
            packagePage.packageSelector.selectPackage(data.chargeTerm, rcCcPackageFolder, rcCcPackage);
            packagePage.packageSelector.selectPackage(data.chargeTerm, engageDigitalPackageFolder, engageDigitalPackage);

            cartPage.openTab();

            allMainLicenseDataIds = checkAndGetMainLicenseDataIdList();
            allMainLicenseDataIds.forEach(dataId ->
                    cartPage.getQliFromCartByDataId(dataId)
                            .getCartItemElement()
                            .shouldBe(visible)
            );

            cartPage.saveChanges();
        });

        //  CRM-30294
        step("5. Check that the corresponding QuoteLineItem records are created " +
                "for all automatically added main licenses for the Master Quote", () -> {
            var masterQliList = enterpriseConnectionUtils.query(
                    "SELECT Product2.ExtID__c " +
                            "FROM QuoteLineItem " +
                            "WHERE QuoteId = '" + wizardPage.getSelectedQuoteId() + "' " +
                            "AND Product2.ExtID__c IN " + getStringListAsString(allMainLicenseDataIds),
                    QuoteLineItem.class);

            for (var mainLicenseDataId : allMainLicenseDataIds) {
                var masterQli = masterQliList.stream()
                        .filter(qli -> qli.getProduct2().getExtID__c().equals(mainLicenseDataId))
                        .findFirst();
                assertThat(masterQli)
                        .as("Master QuoteLineItem record for a Main license with data-id='" + mainLicenseDataId + "'")
                        .isPresent();
            }
        });

        //  CRM-30294
        step("6. Check that the corresponding QuoteLineItem records are created " +
                "for all automatically added main licenses for the Technical Quotes", () -> {
            var techQliList = enterpriseConnectionUtils.query(
                    "SELECT Product2.ExtID__c " +
                            "FROM QuoteLineItem " +
                            "WHERE Quote.MasterQuote__c = '" + wizardPage.getSelectedQuoteId() + "' " +
                            "AND Product2.ExtID__c IN " + getStringListAsString(allMainLicenseDataIds),
                    QuoteLineItem.class);

            for (var mainLicenseDataId : allMainLicenseDataIds) {
                var techQli = techQliList.stream()
                        .filter(qli -> qli.getProduct2().getExtID__c().equals(mainLicenseDataId))
                        .findFirst();
                assertThat(techQli)
                        .as("Technical QuoteLineItem record for a Main license with data-id='" + mainLicenseDataId + "'")
                        .isPresent();
            }
        });
    }

    /**
     * Open a Group and a Subgroup in the given Service for the Product under test,
     * and also check the Subgroups for the Product's Group.
     *
     * @param product   expected product's test data
     * @param subgroups list of the expected subgroups for the product's group
     *                  (e.g. ["Main", "Additional DLs"] for the "Services" of the "Office" service)
     */
    private void checkProductAndSubgroups(Product product, List<String> subgroups) {
        step("Open the '" + product.group + "' group and '" + product.subgroup + "' subgroup for the '" + product.serviceName + "', " +
                "and check that '" + product.name + "' is shown there", () -> {
            productsPage.findProduct(product).getSelf().shouldBe(visible);
        });

        step("Check the subgroups for the '" + product.group + "' group of the '" + product.serviceName + "' service", () -> {
            productsPage.subgroupNames.shouldHave(exactTexts(subgroups));
        });
    }

    /**
     * Get list of Main_License__c field values for all selected services.
     * <p>
     * The list is filled with values of Package_Main_Licenses__c.Main_License__c field
     * regarding selected package's Id is contained in Package_Main_Licenses__c.Package_ID__c column or not
     * (if not, then the list is filled with default Main_License__c value for a selected service).
     *
     * @return list of Main_License__c for all selected services
     */
    private List<String> checkAndGetMainLicenseDataIdList() {
        var mainLicensesDataIdList = new ArrayList<String>();

        step("Check that corresponding Package_Main_Licenses__c records exist for all the selected packages", () -> {
            var mainLicensesList = enterpriseConnectionUtils.query(
                    "SELECT Package_ID__c, Main_License__c, Package_Group__c " +
                            "FROM Package_Main_Licenses__c",
                    Package_Main_Licenses__c.class);
            var mainLicensePackagesList = mainLicensesList.stream()
                    .map(Package_Main_Licenses__c::getPackage_ID__c)
                    .toList();

            packageFolderNameToPackageMap.forEach((serviceName, pkg) -> {
                var packageMainLicense = mainLicensesList
                        .stream()
                        .filter(pml -> mainLicensePackagesList.contains(pkg.id)
                                ? pkg.id.equals(pml.getPackage_ID__c())
                                : serviceName.equals(pml.getPackage_Group__c()) && pml.getPackage_ID__c() == null)
                        .findFirst();
                assertThat(packageMainLicense)
                        .as("Package_Main_Licenses__c record for service = " + serviceName + ", packageId = " + pkg.id)
                        .isPresent();

                mainLicensesDataIdList.add(packageMainLicense.get().getMain_License__c());
            });
        });

        return mainLicensesDataIdList;
    }
}
