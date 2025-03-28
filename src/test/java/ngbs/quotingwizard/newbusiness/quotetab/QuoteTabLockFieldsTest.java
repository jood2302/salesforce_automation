package ngbs.quotingwizard.newbusiness.quotetab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.cartPage;
import static base.Pages.quotePage;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.FIELD_IS_REQUIRED_ERROR;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.AGREEMENT_QUOTE_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.QUOTE_QUOTE_TYPE;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("QuoteTab")
public class QuoteTabLockFieldsTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;

    //  Test data
    private final String noneInitialTerm;
    private final String validInitialTerm;
    private final String validRenewalTerm;
    private final Product dlUnlimited;
    private final Product phoneToAdd;

    public QuoteTabLockFieldsTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/Unify_Office_Monthly_Contract_US.json",
                Dataset.class);

        steps = new Steps(data);

        var contractTerms = data.packageFolders[0].packages[0].contractTerms;
        noneInitialTerm = contractTerms.initialTerm[0];
        validInitialTerm = contractTerms.initialTerm[1];
        validRenewalTerm = contractTerms.renewalTerm;
        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        phoneToAdd = data.getProductByDataName("LC_HD_591");
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-12296")
    @DisplayName("CRM-12296 - Switching Quote Stage locks Fields that require Approval on change")
    @Description("Verify that Quote Stage cannot be switched if the Quote requires Approval")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select package for it, save changes, and add products on the Add Products tab", () -> {
            steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addProductsOnProductsTab(data.getNewProductsToAdd());
        });

        step("2. Open the Price tab, assign the added phone to the DL and save the changes", () -> {
            cartPage.openTab();
            steps.cartTab.assignDevicesToDLAndSave(phoneToAdd.name, dlUnlimited.name, steps.quoteWizard.localAreaCode,
                    phoneToAdd.quantity);
        });

        step("3. Open the Quote Details tab, populate Main Area Code, set Start Date, " +
                "populate Initial Term field with value = 'None', " +
                "verify that Auto-Renewal checkbox is checked, and save the changes", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.setDefaultStartDate();
            quotePage.initialTermPicklist.selectOptionContainingText(noneInitialTerm);
            quotePage.autoRenewalCheckbox.shouldBe(checked);
            quotePage.saveChanges();
        });

        step("4. Switch Quote Stage to 'Agreement', " +
                "and verify that 'Initial Term' field is disabled and highlighted as it is required, " +
                "and that 'Save' button is disabled", () -> {
            quotePage.stagePicklist.shouldBe(enabled).selectOptionContainingText(AGREEMENT_QUOTE_TYPE);

            quotePage.initialTermPicklist.shouldBe(disabled);
            quotePage.initialTermSection.shouldHave(text(FIELD_IS_REQUIRED_ERROR));
            quotePage.saveButton.shouldBe(disabled);
        });

        step("5. Switch Quote Stage to 'Quote', select valid 'Initial Term' and 'Renewal Term', and save the changes", () -> {
            quotePage.stagePicklist.selectOptionContainingText(QUOTE_QUOTE_TYPE);

            quotePage.initialTermPicklist.selectOptionContainingText(validInitialTerm);
            quotePage.renewalTermPicklist.selectOptionContainingText(validRenewalTerm);

            quotePage.saveChanges();
        });

        step("6. Switch Quote Stage to 'Agreement' and verify that 'Initial Term', 'Auto-Renewal' " +
                "and 'Special Terms' fields are disabled", () -> {
            quotePage.stagePicklist.selectOptionContainingText(AGREEMENT_QUOTE_TYPE);

            quotePage.initialTermPicklist.shouldBe(disabled);
            quotePage.autoRenewalCheckbox.shouldBe(disabled);

            quotePage.footer.billingDetailsAndTermsButton.click();
            quotePage.billingDetailsAndTermsModal.specialTermsPicklist.shouldBe(disabled);
        });
    }
}
