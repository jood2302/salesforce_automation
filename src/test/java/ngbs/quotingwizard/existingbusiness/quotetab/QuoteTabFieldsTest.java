package ngbs.quotingwizard.existingbusiness.quotetab;

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
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Selenide.refresh;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("NGBS")
@Tag("QuoteTab")
public class QuoteTabFieldsTest extends BaseTest {
    private final Steps steps;
    private final QuoteTabSteps quoteTabSteps;

    //  Test data
    private final Product dlUnlimited;
    private final Product phoneToAdd;

    public QuoteTabFieldsTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_Contract_163073013.json",
                Dataset.class);

        steps = new Steps(data);
        quoteTabSteps = new QuoteTabSteps();

        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        phoneToAdd = data.getProductByDataName("LC_HD_687");
    }

    @BeforeEach
    public void setUpTest() {
        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.generateBillingAccount();
            steps.ngbs.stepCreateContractInNGBS();
        }

        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-12433")
    @TmsLink("CRM-12311")
    @DisplayName("CRM-12433 - Contract Term fields are disabled if no contract is selected. Existing Business. \n" +
            "CRM-12033 - Warning About Assignment is available on the Existing Business Opportunity. \n" +
            "CRM-12311 - Self-Provisioned and Provisioning details fields are locked " +
            "if the Opportunity is closed or Submitted for Approval. Existing Business.")
    @Description("CRM-12433 - Verify that Contract Term fields are disabled if contract isn't selected. \n" +
            "CRM-12311 - Verify that if Opportunity is closed or quote is Submitted for Approval " +
            "then Self-Provisioned and Provisioning Details fields are locked.")
    public void test() {
        step("1. Open the Quote Wizard for the Engage Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        //  CRM-12433
        step("2. Open the Quote Details tab, " +
                "and check that Initial Term, Renewal Term and Auto-Renewal fields are disabled", () -> {
            quotePage.openTab();

            quotePage.initialTermPicklist.shouldBe(disabled);
            quotePage.renewalTermPicklist.shouldBe(disabled);
            quotePage.autoRenewalCheckbox.shouldBe(disabled);
        });

        step("3. Open the Select Package tab, remove Contract from selected package, and save changes", () -> {
            packagePage.openTab();
            packagePage.packageSelector.setContractSelected(false);
            packagePage.saveChanges();
        });

        //  CRM-12433
        step("4. Open the Quote Details tab, " +
                "and check that Initial Term and Renewal Term are disabled, and Auto-Renewal is hidden", () -> {
            quotePage.openTab();

            quotePage.initialTermPicklist.shouldBe(disabled);
            quotePage.renewalTermPicklist.shouldBe(disabled);
            quotePage.autoRenewalCheckbox.shouldBe(hidden);
        });

        //  CRM-12433
        step("5. Check Quote.Auto_Renewal__c field value", () ->
                quoteTabSteps.stepCheckQuoteAutoRenewal(steps.quoteWizard.opportunity.getId(), quoteTabSteps.autoRenewalNoContract)
        );

        step("6. Open the Select Package tab, select contract for the selected package", () -> {
            packagePage.openTab();
            packagePage.packageSelector.setContractSelected(true);
        });

        step("7. Open the Add Products tab and the phone from there", () ->
                steps.quoteWizard.addProductsOnProductsTab(phoneToAdd)
        );

        step("8. Open the Price tab, set up quantities and discounts to products, assign devices to DLs", () -> {
            cartPage.openTab();
            steps.cartTab.setUpQuantities(dlUnlimited, phoneToAdd);
            steps.cartTab.setUpDiscounts(dlUnlimited);
            steps.cartTab.assignDevicesToDL(phoneToAdd.name, dlUnlimited.name, steps.quoteWizard.localAreaCode,
                    phoneToAdd.quantity);
        });

        step("9. Open the Quote Details tab, set Discount Justification, and save changes", () -> {
            quotePage.openTab();
            quotePage.discountJustificationTextArea.setValue(TEST_STRING).unfocus();
            quotePage.saveChanges();
        });

        step("10. Close the Opportunity via API", () ->
                steps.quoteWizard.stepCloseOpportunity(steps.quoteWizard.opportunity)
        );

        //  CRM-12311
        step("11. Refresh the Quote Wizard, open the Quote Details tab, " +
                "and make sure that 'Self-Provisioned' and 'Provisioning Details' fields are disabled", () -> {
            refresh();
            wizardPage.waitUntilLoaded();

            quotePage.openTab();
            quotePage.selfProvisionedCheckbox.shouldBe(disabled);
            quotePage.provisioningDetailsTextArea.shouldBe(disabled);
        });
    }
}
