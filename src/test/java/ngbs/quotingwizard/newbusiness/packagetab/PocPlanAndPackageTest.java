package ngbs.quotingwizard.newbusiness.packagetab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.components.packageselector.Package.PACKAGE_INFO_ATTRIBUTE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.POC_QUOTE_RECORD_TYPE;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("PDV")
@Tag("NGBS")
@Tag("POC")
@Tag("PackageTab")
public class PocPlanAndPackageTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private String opportunityId;

    //  Test data
    private final String officeService;
    private final String engageDigitalService;
    private final Package mvpRegularPackage;
    private final Package mvpPocPackage;
    private final Package engageDigitalPackage;
    private final List<Package> expectedPocPackages;

    public PocPlanAndPackageTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_RegularAndPOC_Monthly_Contract_ED_EV_Standalone_NB.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        officeService = data.packageFolders[0].name;
        engageDigitalService = data.packageFolders[1].name;
        mvpRegularPackage = data.packageFolders[0].packages[0];
        mvpPocPackage = data.packageFolders[0].packages[1];
        engageDigitalPackage = data.packageFolders[1].packages[0];

        var rcEventsPocPackage = data.packageFolders[3].packages[0];
        expectedPocPackages = List.of(mvpPocPackage, rcEventsPocPackage);
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        opportunityId = steps.quoteWizard.opportunity.getId();

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-9337")
    @TmsLink("CRM-11684")
    @TmsLink("CRM-21370")
    @DisplayName("CRM-9337 - POC Packages are shown on POC Quotes. \n" +
            "CRM-11684 - User can create a POC Quote with a chosen Package and a Plan. New Business. \n" +
            "CRM-21370 - New Business | No buttons are enabled after New POC Quote Creation")
    @Description("CRM-9337 - Verify that only POC Packages are shown on POC Quotes on the Select Package tab in the Quoting Wizard. \n" +
            "CRM-11684 - Verify that it's possible to create a POC Quote for the Opportunity that already has MultiProduct Quote (MVP+ED/EV/CC) " +
            "(multi selection is not possible, Service selector has only Office service available). \n" +
            "CRM-21370 - Verify that Save buttons behave correctly. No buttons are enabled after New POC Quote Creation")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new POC Quote", () -> {
            steps.quoteWizard.openQuoteWizardForNewPocQuoteDirect(opportunityId);
        });

        //  CRM-9337, CRM-11684
        step("2. Check the Select Package tab for the POC Quote, select a POC package, save changes, " +
                "and check that POC Quote is created and the package/charge term is selected for it", () -> {
            checkPackagePageAndSelectPocPackageTestSteps();
        });

        //  CRM-21370
        step("3. Check that 'Save and Continue' button on the Select Package tab, " +
                "'Save Changes' buttons on the Price and Quote Details tabs are disabled, " +
                "and tab switching performs without any confirmation windows", () -> {
            packagePage.saveAndContinueButton.shouldBe(disabled);

            cartPage.openTab();
            cartPage.saveButton.shouldBe(disabled);

            quotePage.openTab();
            quotePage.saveButton.shouldBe(disabled);
        });

        step("4. Delete the current POC Quote via API", () -> {
            var pocQuoteId = wizardPage.getSelectedQuoteId();
            enterpriseConnectionUtils.deleteByIds(pocQuoteId);
        });

        step("5. Open the Quote Wizard to add a new Sales Quote, " +
                "select MVP and Engage Digital packages for it, and save changes", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(opportunityId);
            packagePage.packageSelector.selectPackage(data.chargeTerm, officeService, mvpRegularPackage);
            packagePage.packageSelector.selectPackage(data.chargeTerm, engageDigitalService, engageDigitalPackage);
            packagePage.saveChanges();
        });

        step("6. Open the Quote Selection page of the Quote Wizard and add a new POC Quote", () -> {
            steps.quoteWizard.openQuoteWizardDirect(opportunityId);
            steps.quoteWizard.addNewPocQuote();
        });

        //  CRM-9337, CRM-11684
        step("7. Check the Select Package tab for the POC Quote, select a POC package, save changes, " +
                "and check that POC Quote is created and the package/charge term is selected for it", () -> {
            checkPackagePageAndSelectPocPackageTestSteps();
        });
    }

    /**
     * Check the contents of the Select Package tab for the new POC Quote,
     * select a POC package, and check the created POC Quote.
     */
    private void checkPackagePageAndSelectPocPackageTestSteps() {
        step("Check that only POC Packages are shown, " +
                "Contract checkbox is disabled and unchecked, " +
                "and Multi-Product selection is impossible on the Select Package tab", () -> {
            var availablePackagesListSize = packagePage.packageSelector.allPackages.size();
            packagePage.packageSelector.allPackages
                    .filterBy(attributeMatching(PACKAGE_INFO_ATTRIBUTE, "^POC_.+_.+$"))
                    .shouldHave(size(availablePackagesListSize));

            packagePage.packageSelector.packageFilter.contractSelectInput.should(be(disabled), not(be(checked)));
            packagePage.packageSelector.packageFolders.shouldHave(size(expectedPocPackages.size()));
        });

        step("Select a POC package, save the changes, and check that POC Quote is created", () -> {
            packagePage.packageSelector.selectPackage(data.chargeTerm, officeService, mvpPocPackage);
            packagePage.saveChanges();

            packagePage.packageSelector.packageFilter.getChargeTermInput(data.chargeTerm).shouldBe(selected);
            packagePage.packageSelector.getSelectedPackage().getName()
                    .shouldHave(exactTextCaseSensitive(mvpPocPackage.getFullName()));

            var createdPocQuotes = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + opportunityId + "' " +
                            "AND RecordType.Name = '" + POC_QUOTE_RECORD_TYPE + "'",
                    Quote.class);
            assertThat(createdPocQuotes.size())
                    .as("Number of POC Quotes on the Opportunity")
                    .isEqualTo(1);
        });
    }
}
