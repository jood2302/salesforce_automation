package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.codeborne.selenide.Condition.disabled;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("PDV")
@Tag("Buttons")
public class TabSwitchingWithoutConfirmationTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;

    //  Test data
    private final String officeService;
    private final Package initialRegularPackage;
    private final Package differentRegularPackage;
    private final Package initialPocPackage;
    private final Package differentPocPackage;

    public TabSwitchingWithoutConfirmationTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_Contract_1PhoneAnd1DL_RegularAndPOC.json",
                Dataset.class);
        steps = new Steps(data);

        officeService = data.packageFolders[0].name;
        initialRegularPackage = data.packageFolders[0].packages[0];
        differentRegularPackage = data.packageFolders[0].packages[3];
        initialPocPackage = data.packageFolders[0].packages[1];
        differentPocPackage = data.packageFolders[0].packages[2];
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-21243")
    @DisplayName("CRM-21243 - 'Save and Continue' button is disabled and the tab is changed without Changes Confirmation modal window appearing")
    @Description("Verify that 'Save and Continue' is disabled and the tab is changed without Changes Confirmation modal window appearing")
    public void test() {
        step("1. Open the Quote Wizard for the New Business Opportunity to add new Sales Quote, " +
                "select a package for it, save changes, " +
                "and verify that 'Save and Continue' button is disabled, " +
                "open the Price and Quote Details tabs and check that tab switching performs without any confirmation windows", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());

            packagePage.packageSelector.packageFilter.servicePicklist.selectOption(officeService);
            packagePage.packageSelector.selectPackage(data.chargeTerm, officeService, initialRegularPackage);
            packagePage.saveChanges();

            packagePage.saveAndContinueButton.shouldBe(disabled);

            cartPage.openTab();
            quotePage.openTab();
            //  to bypass all the validations on the save action later
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
        });

        step("2. Open the Select Package tab, select a different package, save changes, " +
                "and verify that 'Save and Continue' button is disabled, " +
                "open the Price and Quote Details tabs and check that tab switching performs without any confirmation windows", () -> {
            packagePage.openTab();
            packagePage.packageSelector.selectPackage(data.chargeTerm, officeService, differentRegularPackage);
            packagePage.saveChanges();

            packagePage.saveAndContinueButton.shouldBe(disabled);

            cartPage.openTab();
            quotePage.openTab();
        });

        step("3. Open the Quote Wizard for the New Business Opportunity to add new POC Quote, " +
                "select a package for it, save changes, " +
                "and verify that 'Save and Continue' button is disabled, " +
                "open the Price and Quote Details tabs and check that tab switching performs without any confirmation windows", () -> {
            wizardPage.openPageForNewPocQuote(steps.quoteWizard.opportunity.getId());
            wizardPage.waitUntilLoaded();
            packagePage.packageSelector.waitUntilLoaded();

            packagePage.packageSelector.packageFilter.servicePicklist.selectOption(officeService);
            packagePage.packageSelector.selectPackage(data.chargeTerm, officeService, initialPocPackage);
            packagePage.saveChanges();

            packagePage.saveAndContinueButton.shouldBe(disabled);

            cartPage.openTab();
            quotePage.openTab();
            //  to bypass all the validations on the save action later
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
        });

        step("4. Open the Select Package tab, select a different package, save changes, " +
                "and verify that 'Save and Continue' button is disabled, " +
                "open the Price and Quote Details tabs and check that tab switching performs without any confirmation windows", () -> {
            packagePage.openTab();
            packagePage.packageSelector.selectPackage(data.chargeTerm, officeService, differentPocPackage);
            packagePage.saveChanges();

            packagePage.saveAndContinueButton.shouldBe(disabled);

            cartPage.openTab();
            quotePage.openTab();
        });
    }
}
