package ngbs.quotingwizard.existingbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NgbsQuotingWizardFooter.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.NONE;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("QTFooter")
@Tag("UQT")
public class GlobalFooterOnTabsTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;

    //  Test data
    private final String initialTerm;
    private final String renewalTerm;
    private final String annualChargeTerm;

    public GlobalFooterOnTabsTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_Contract_163075013.json",
                Dataset.class);
        steps = new Steps(data);

        initialTerm = data.packageFolders[0].packages[0].contractTerms.initialTerm[0];
        renewalTerm = data.packageFolders[0].packages[0].contractTerms.renewalTerm;
        annualChargeTerm = "Annual";
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
    @TmsLink("CRM-26297")
    @DisplayName("CRM-26297 - Global Footer layout. Existing Business.")
    @Description("Verify that Global Footer is shown on every tab (except the Select Package tab) " +
            "of the Quote Wizard for Existing business Accounts")
    public void test() {
        step("1. Open the Quote Wizard for the Existing Business Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        step("2. Open the Select Package tab and check that the Global Footer is not visible on it", () -> {
            packagePage.openTab();
            quotingWizardFooter.footerContainer.shouldBe(hidden);
        });

        step("3. Open the Add Products tab and check the Global Footer, its fields and values", () -> {
            productsPage.openTab();
            checkGlobalFooterForMonthlyTerm();
        });

        step("4. Open the Price tab and check the Global Footer, its fields and values", () -> {
            cartPage.openTab();
            checkGlobalFooterForMonthlyTerm();
        });

        step("5. Open the Quote Details tab and check the Global Footer, its fields and values", () -> {
            quotePage.openTab();
            checkGlobalFooterForMonthlyTerm();
        });

        step("6. Open the Select Package tab, select the same package with the Annual charge term", () -> {
            packagePage.openTab();
            packagePage.packageSelector.packageFilter.selectChargeTerm(annualChargeTerm);
        });

        step("7. Open the Add Products tab and check the Global Footer, its fields and values", () -> {
            productsPage.openTab();
            checkGlobalFooterForAnnualTerm();
        });

        step("8. Open the Price tab and check the Global Footer, its fields and values", () -> {
            cartPage.openTab();
            checkGlobalFooterForAnnualTerm();
        });

        step("9. Open the Quote Details tab and check the Global Footer, its fields and values", () -> {
            quotePage.openTab();
            checkGlobalFooterForAnnualTerm();
        });
    }

    /**
     * Check the Global Footer's fields and their values in the Unified Quoting Tool
     * for the Monthly charge term.
     */
    private void checkGlobalFooterForMonthlyTerm() {
        quotingWizardFooter.footerContainer.shouldBe(visible);
        quotingWizardFooter.contract.shouldBe(visible);
        quotingWizardFooter.paymentPlan.shouldHave(exactTextCaseSensitive(data.chargeTerm));
        quotingWizardFooter.initialTerm.shouldHave(exactText(initialTerm));
        quotingWizardFooter.renewalTerm.shouldHave(exactText(renewalTerm));
        quotingWizardFooter.freeServiceCredit.shouldHave(exactTextCaseSensitive(NONE));
        quotingWizardFooter.specialShippingTerms.shouldBe(visible);

        quotingWizardFooter.newRecurringChargesLabel.shouldHave(exactTextCaseSensitive(NEW_MONTHLY_RECURRING_CHARGES));
        quotingWizardFooter.newRecurringCharges.shouldBe(visible);
        quotingWizardFooter.currentRecurringChargesLabel.shouldHave(exactTextCaseSensitive(CURRENT_MONTHLY_RECURRING_CHARGES));
        quotingWizardFooter.currentRecurringCharges.shouldBe(visible);
        quotingWizardFooter.changeInRecurringCharges.shouldBe(visible);
        quotingWizardFooter.newDiscountLabel.shouldHave(exactTextCaseSensitive(NEW_MONTHLY_DISCOUNT));
        quotingWizardFooter.newDiscount.shouldBe(visible);
        quotingWizardFooter.currentDiscountLabel.shouldHave(exactTextCaseSensitive(CURRENT_MONTHLY_DISCOUNT));
        quotingWizardFooter.currentDiscount.shouldBe(visible);
        quotingWizardFooter.changeInDiscount.shouldBe(visible);
        quotingWizardFooter.costOfOneTimeItems.shouldBe(visible);
    }

    /**
     * Check the Global Footer's fields and their values in the Unified Quoting Tool
     * for the Annual charge term.
     */
    private void checkGlobalFooterForAnnualTerm() {
        quotingWizardFooter.footerContainer.shouldBe(visible);
        quotingWizardFooter.contract.shouldBe(visible);
        quotingWizardFooter.paymentPlan.shouldHave(exactTextCaseSensitive(annualChargeTerm));
        quotingWizardFooter.initialTerm.shouldHave(exactText(initialTerm));
        quotingWizardFooter.renewalTerm.shouldHave(exactText(renewalTerm));
        quotingWizardFooter.freeServiceCredit.shouldHave(exactTextCaseSensitive(NONE));
        quotingWizardFooter.specialShippingTerms.shouldBe(visible);

        quotingWizardFooter.newRecurringChargesLabel.shouldHave(exactTextCaseSensitive(NEW_ANNUAL_RECURRING_CHARGES));
        quotingWizardFooter.newRecurringCharges.shouldBe(visible);
        quotingWizardFooter.currentRecurringChargesLabel.shouldHave(exactTextCaseSensitive(CURRENT_MONTHLY_RECURRING_CHARGES));
        quotingWizardFooter.currentRecurringCharges.shouldBe(visible);
        quotingWizardFooter.changeInRecurringCharges.shouldBe(visible);
        quotingWizardFooter.newDiscountLabel.shouldHave(exactTextCaseSensitive(NEW_ANNUAL_DISCOUNT));
        quotingWizardFooter.newDiscount.shouldBe(visible);
        quotingWizardFooter.currentDiscountLabel.shouldHave(exactTextCaseSensitive(CURRENT_MONTHLY_DISCOUNT));
        quotingWizardFooter.currentDiscount.shouldBe(visible);
        quotingWizardFooter.changeInDiscount.shouldBe(visible);
        quotingWizardFooter.costOfOneTimeItems.shouldBe(visible);
    }
}
