package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.Map;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.refresh;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("QuoteTab")
@Tag("PriceTab")
@Tag("ProductsTab")
@Tag("Multiproduct-Lite")
public class MultiProductQuoteButtonsTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User salesUserWithEnableDocusignClmPS;

    //  Test data
    private final Map<String, Package> packageFolderNameToPackageMap;

    public MultiProductQuoteButtonsTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_ED_EV_CC_ProServ_Monthly_Contract.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        packageFolderNameToPackageMap = Map.of(
                data.packageFolders[0].name, data.packageFolders[0].packages[0],
                data.packageFolders[1].name, data.packageFolders[1].packages[0]
        );
    }

    @BeforeEach
    public void setUpTest() {
        step("Find a user with 'Sales Rep - Lightning' profile and with 'Enable DocuSign CLM Access' permission set", () -> {
            salesUserWithEnableDocusignClmPS = getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    .withPermissionSet(ENABLE_DOCUSIGN_CLM_ACCESS_PS)
                    .execute();
        });

        steps.salesFlow.createAccountWithContactAndContactRole(salesUserWithEnableDocusignClmPS);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesUserWithEnableDocusignClmPS);

        step("Login as a user with 'Sales Rep - Lightning' profile and with 'Enable DocuSign CLM Access' permission set", () -> {
            steps.sfdc.initLoginToSfdcAsTestUser(salesUserWithEnableDocusignClmPS);
        });
    }

    @Test
    @TmsLink("CRM-31808")
    @TmsLink("CRM-32736")
    @TmsLink("CRM-32737")
    @DisplayName("CRM-31808 - Header buttons for New Business on 'Quote Details' Tab. \n" +
            "CRM-32736 - Header buttons for New Business on 'Price' Tab. \n" +
            "CRM-32737 - Header buttons for New Business on 'Select Package' & 'Add Products' Tabs.")
    @Description("CRM-31808 - Verify that for New Business Multiproduct Quote on 'Quote Details' tab correct header buttons are shown. \n" +
            "CRM-32736 - Verify that for New Business Multiproduct Quote on 'Price' tab correct header buttons are shown. \n" +
            "CRM-32737 - Verify that for New Business Multiproduct Quote on 'Select Package' & 'Add Products' Tabs " +
            "correct header buttons are shown.")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new Sales Quote, " +
                "select Office and RingCentral Contact Center packages for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityForMultiProduct(steps.quoteWizard.opportunity.getId(), packageFolderNameToPackageMap)
        );

        //  CRM-32737
        step("2. Check that 'Initiate ProServ', 'Initiate CC ProServ' and 'Create a Case' buttons " +
                "are displayed on the Select Package tab", () -> {
            packagePage.initiateProServButton.shouldBe(visible);
            packagePage.initiateCcProServButton.shouldBe(visible);
            packagePage.createCaseButton.shouldBe(visible);
        });

        //  CRM-32737
        step("3. Open the Add Products tab " +
                "and check that 'Initiate ProServ', 'Initiate CC ProServ' and 'Create a Case' buttons are displayed on it", () -> {
            productsPage.openTab();
            productsPage.initiateProServButton.shouldBe(visible);
            productsPage.initiateCcProServButton.shouldBe(visible);
            productsPage.createCaseButton.shouldBe(visible);
        });

        //  CRM-32736
        step("4. Open the Price tab and check the header buttons on it", () -> {
            cartPage.openTab();
            cartPage.initiateProServButton.shouldBe(visible);
            cartPage.initiateCcProServButton.shouldBe(visible);
            cartPage.addTaxesButton.shouldBe(visible);
            cartPage.submitForApprovalButton.shouldBe(visible);
            cartPage.generatePdfButton.shouldBe(visible);
            cartPage.moreActionsButton.hover();
            cartPage.copyQuoteButton.shouldBe(visible);
            cartPage.createCaseButton.shouldBe(visible);
        });

        //  CRM-31808
        step("5. Set Opportunity.IsDocusignEnabled__c = true via API, and refresh the Quote Wizard", () -> {
            steps.quoteWizard.opportunity.setIsDocusignCLMEnabled__c(true);
            enterpriseConnectionUtils.update(steps.quoteWizard.opportunity);

            refresh();
            wizardPage.waitUntilLoaded();
        });

        //  CRM-31808
        step("6. Open the Quote Details tab and check the header buttons on it", () -> {
            quotePage.openTab();
            quotePage.initiateProServButton.shouldBe(visible);
            quotePage.initiateCcProServButton.shouldBe(visible);
            quotePage.generatePdfButton.shouldBe(visible);
            quotePage.createContractButton.shouldBe(visible);
            quotePage.moreActionsButton.hover();
            quotePage.copyQuoteButton.shouldBe(visible);
            quotePage.createCaseButton.shouldBe(visible);
        });

        //  CRM-31808
        step("7. Create a new POC Quote and save changes", () -> {
            steps.quoteWizard.openQuoteWizardForNewPocQuoteDirect(steps.quoteWizard.opportunity.getId());
            packagePage.saveChanges();
        });

        //  CRM-31808
        step("8. Open the Quote Details tab and check the buttons of the Quote Wizard for the POC Quote", () -> {
            quotePage.openTab();
            quotePage.createPocApprovalButton.shouldBe(visible);
            quotePage.createContractButton.shouldBe(visible);
            quotePage.moreActionsButton.hover();
            quotePage.copyQuoteButton.shouldBe(visible);
            quotePage.createCaseButton.shouldBe(visible);
        });

        //  CRM-32736
        step("9. Open the Price tab and check the buttons of the Quote Wizard for the POC Quote", () -> {
            cartPage.openTab();
            cartPage.generatePdfButton.shouldBe(visible);
            cartPage.moreActionsButton.hover();
            cartPage.copyQuoteButton.shouldBe(visible);
            cartPage.createCaseButton.shouldBe(visible);
        });
    }
}
