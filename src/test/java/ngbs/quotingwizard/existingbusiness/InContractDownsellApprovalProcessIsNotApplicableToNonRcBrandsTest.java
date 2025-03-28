package ngbs.quotingwizard.existingbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.cartPage;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartPage.APPROVED_APPROVAL_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.QuoteFactory.createActiveSalesAgreement;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("Avaya")
@Tag("Quote")
public class InContractDownsellApprovalProcessIsNotApplicableToNonRcBrandsTest extends BaseTest {
    private final Steps steps;

    //  Test data
    private final Product digitalLineUnlimited;
    private final String initialTerm;
    private final Integer decreasedDlQuantity;

    public InContractDownsellApprovalProcessIsNotApplicableToNonRcBrandsTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/Avaya_Office_Monthly_Contract_91964013.json",
                Dataset.class);
        steps = new Steps(data);

        digitalLineUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        initialTerm = data.getInitialTerm();
        decreasedDlQuantity = digitalLineUnlimited.quantity - 5;
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

        step("Create an Active Sales Agreement for the test Account's Opportunity via API", () -> {
            createActiveSalesAgreement(steps.quoteWizard.opportunity, initialTerm);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-33013")
    @DisplayName("CRM-33013 - 'In contract downsell' GOA Approval Process is not applicable to the non-RC brands")
    @Description("Verify that the 'In contract downsell' approval process is not applicable to the non-RC Brands")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        step("2. Open the Price tab, decrease New Quantity of DigitalLine Unlimited by 5, save changes " +
                "and check Approval Status", () -> {
            //  (-30.99) for 1 DL * 5 = -154.95 USD <= -100 USD in MRR change (should be less than InContractDownsell__c.Threshold__c)
            cartPage.setNewQuantityForQLItem(digitalLineUnlimited.name, decreasedDlQuantity);
            cartPage.saveChanges();
            cartPage.approvalStatus.shouldHave(exactTextCaseSensitive(APPROVED_APPROVAL_STATUS));
        });
    }
}
