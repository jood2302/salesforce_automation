package ngbs.quotingwizard.newbusiness.quotetab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import ngbs.quotingwizard.QuoteTabSteps;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.refresh;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("NGBS")
@Tag("QuoteTab")
public class QuoteTabFieldsTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final QuoteTabSteps quoteTabSteps;

    //  Test data
    private final Product dlUnlimited;
    private final Product polycomPhone;

    public QuoteTabFieldsTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_Contract_1PhoneAnd1DL_RegularAndPOC.json",
                Dataset.class);

        steps = new Steps(data);
        quoteTabSteps = new QuoteTabSteps();

        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        polycomPhone = data.getProductByDataName("LC_HDR_619");
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-12432")
    @TmsLink("CRM-13119")
    @TmsLink("CRM-13118")
    @TmsLink("CRM-12309")
    @DisplayName("CRM-12432 - Contract Term fields are disabled if no contract is selected. New Business. \n" +
            "CRM-13119 - If Contract is deselected Auto-Renewal is set to false. \n" +
            "CRM-13118 - If Contract isn't selected Auto-Renewal is set to false. \n" +
            "CRM-12309 - Self-Provisioned and Provisioning details fields locking if the Opportunity is closed. New Business.")
    @Description("CRM-12432 - Verify that Contract Term fields are disabled if contract isn't selected. \n" +
            "CRM-13119 - Verify that if the Contract is deselected on the Select Package Tab, Auto-Renewal Flag is set to false. \n" +
            "CRM-13118 - Verify that if no Contract is selected on the Select Package Tab, Auto-Renewal Flag is set to false. \n" +
            "CRM-12309 - Verify that if Opportunity is closed, Self-Provisioned and Provisioning Details fields are locked")
    public void test() {
        step("1. Open the Quote Wizard for New Business Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        //  CRM-12432, CRM-13119
        step("2. Open the Quote Details tab, check Quote fields, set Main Area Code, and save changes", () -> {
            quotePage.openTab();

            quotePage.initialTermPicklist.shouldBe(enabled);
            quotePage.renewalTermPicklist.shouldBe(enabled);
            quotePage.autoRenewalCheckbox.shouldBe(enabled, checked);

            //  Set Area codes to resolve validation errors
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.saveChanges();
        });

        //  CRM-13119
        step("3. Check Quote's 'Auto_Renewal__c' value", () ->
                quoteTabSteps.stepCheckQuoteAutoRenewal(steps.quoteWizard.opportunity.getId(),
                        quoteTabSteps.autoRenewalWithContract)
        );

        step("4. Open the Select Package tab, remove Contract from selected package, and save changes", () -> {
            packagePage.openTab();
            packagePage.packageSelector.setContractSelected(false);
            packagePage.saveChanges();
        });

        //  CRM-12432, CRM-13119 and CRM-13118
        step("5. Open the Quote Details tab and check Quote fields", () -> {
            quotePage.openTab();

            quotePage.initialTermPicklist.shouldBe(disabled);
            quotePage.renewalTermPicklist.shouldBe(disabled);
            quotePage.autoRenewalCheckbox.shouldBe(hidden);
        });

        //  CRM-13118 and CRM-13119
        step("6. Check Quote's 'Auto_Renewal__c' value", () ->
                quoteTabSteps.stepCheckQuoteAutoRenewal(steps.quoteWizard.opportunity.getId(),
                        quoteTabSteps.autoRenewalNoContract)
        );

        step("7. Open the Select Package tab, select contract for the selected package, and save changes", () -> {
            packagePage.openTab();
            packagePage.packageSelector.setContractSelected(true);
            packagePage.saveChanges();
        });

        step("8. Open the Add Products Tab and add products to cart", () ->
                steps.quoteWizard.addProductsOnProductsTab(data.getNewProductsToAdd())
        );

        step("9. Open the Price Tab, add discounts to the products, and assign devices to DLs", () -> {
            cartPage.openTab();
            steps.cartTab.setUpDiscounts(dlUnlimited);
            steps.cartTab.assignDevicesToDLWithoutSettingAreaCode(polycomPhone.name, dlUnlimited.name, polycomPhone.quantity);
        });

        step("10. Open the Quote Details tab, set Discount Justification, and save changes", () -> {
            quotePage.openTab();
            quotePage.discountJustificationTextArea.setValue(TEST_STRING);
            quotePage.saveChanges();
        });

        step("11. Close the Opportunity via API", () ->
                steps.quoteWizard.stepCloseOpportunity(steps.quoteWizard.opportunity)
        );

        //  CRM-12309
        step("12. Refresh the Quote Wizard, open the Quote Details tab, " +
                "and make sure that 'Self-Provisioned' and 'Provisioning Details' fields are disabled", () -> {
            refresh();
            wizardPage.waitUntilLoaded();

            quotePage.openTab();
            quotePage.selfProvisionedCheckbox.shouldBe(disabled);
            quotePage.provisioningDetailsTextArea.shouldBe(disabled);
        });
    }
}
