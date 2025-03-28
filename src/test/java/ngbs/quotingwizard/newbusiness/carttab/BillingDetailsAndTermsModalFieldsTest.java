package ngbs.quotingwizard.newbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.cartPage;
import static base.Pages.packagePage;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartPage.FINANCE_REVENUE_DQ_APPROVER;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartPage.NO_APPROVAL_REQUIRED_DQ_APPROVER;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.CollectionCondition.containExactTextsCaseSensitive;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("P1")
@Tag("NGBS")
@Tag("UQT")
public class BillingDetailsAndTermsModalFieldsTest extends BaseTest {
    private final Steps steps;

    private User salesRepUserFromSohoSegment;

    //  Test data
    private final String initialTerm;
    private final String renewalTerm;
    private final String oneFreeMonthServiceCredit;
    private final String twoFreeMonthsServiceCredit;
    private final List<String> expectedApproverLevels;

    public BillingDetailsAndTermsModalFieldsTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_NonContract_1TypeOfDL.json",
                Dataset.class);
        steps = new Steps(data);

        initialTerm = data.packageFolders[0].packages[2].contractTerms.initialTerm[0];
        renewalTerm = data.packageFolders[0].packages[2].contractTerms.renewalTerm;
        oneFreeMonthServiceCredit = "1 Free Month of Service";
        twoFreeMonthsServiceCredit = "2 Free Months of Service";
        expectedApproverLevels = List.of("Finance", "Level 3", "Level 2", "Level 1");
    }

    @BeforeEach
    public void setUpTest() {
        step("Find a user with 'Sales Rep - Lightning' profile and Segment = 'SOHO'", () -> {
            salesRepUserFromSohoSegment = getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    .withSegment(SOHO_SEGMENT)
                    .execute();
        });

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUserFromSohoSegment);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUserFromSohoSegment);

        step("Login as a user with 'Sales Rep - Lightning' profile and Segment = 'SOHO'", () ->
                steps.sfdc.initLoginToSfdcAsTestUser(salesRepUserFromSohoSegment)
        );
    }

    @Test
    @TmsLink("CRM-27885")
    @TmsLink("CRM-7645")
    @TmsLink("CRM-27296")
    @TmsLink("CRM-27297")
    @DisplayName("CRM-27885 - Fields availability in 'Billing Details and Terms' depends on contract on Quote. \n" +
            "CRM-7645 - FSC for accounts w/o contracts. \n" +
            "CRM-27296 - Approval logic for FSC - Sales quote where FSC months <= Initial term years. \n" +
            "CRM-27297 - Approval logic for FSC - Sales quote where FSC months > Initial term years")
    @Description("CRM-27885 - Verify that available fields in 'Billing Details and Terms' modal window " +
            "depend on the Contract checkbox on Sales Quote. \n" +
            "CRM-7645 - Verify that 'Free Service Credit Amount' is not shown for Customers without contracts. \n" +
            "CRM-27296 - Verify that Approval is not required for Special Terms when Number of Free Months is less or equal to the Initial Term (in years). \n" +
            "CRM-27297 - Verify that Approval is required for Special Terms when Number of Free Months is more than the Initial Term (in years)")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new Sales Quote, " +
                        "select a package for it and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        step("2. Open the Price tab, click 'Billing Details and Terms' button in footer and check modal window elements", () -> {
            cartPage.openTab();
            cartPage.footer.billingDetailsAndTermsButton.click();

            //  CRM-27885
            cartPage.billingDetailsAndTermsModal.contractCheckbox.shouldBe(visible);
            cartPage.billingDetailsAndTermsModal.contractSelectInput.shouldNotBe(checked);
            cartPage.billingDetailsAndTermsModal.paymentPlanToggle.shouldBe(visible);
            cartPage.billingDetailsAndTermsModal.initialTermPicklist.shouldBe(hidden);
            cartPage.billingDetailsAndTermsModal.renewalTermPicklist.shouldBe(hidden);
            cartPage.billingDetailsAndTermsModal.freeShippingTermsPicklist.shouldBe(hidden);

            //  CRM-27885, CRM-7645
            cartPage.billingDetailsAndTermsModal.specialTermsPicklist.shouldBe(hidden);
        });

        //  CRM-27885
        step("3. Check Contract checkbox and check modal window elements and close it", () -> {
            cartPage.billingDetailsAndTermsModal.setContractSelected(true);

            cartPage.billingDetailsAndTermsModal.paymentPlanToggle.shouldBe(visible, editable);
            cartPage.billingDetailsAndTermsModal.contractCheckbox.shouldBe(visible);
            cartPage.billingDetailsAndTermsModal.contractSelectInput.shouldBe(editable);
            cartPage.billingDetailsAndTermsModal.initialTermPicklist.shouldBe(visible, editable);
            cartPage.billingDetailsAndTermsModal.renewalTermPicklist.shouldBe(visible, editable);
            cartPage.billingDetailsAndTermsModal.freeShippingTermsPicklist.shouldBe(visible, editable);
            cartPage.billingDetailsAndTermsModal.specialTermsPicklist.shouldBe(visible, editable);
            cartPage.billingDetailsAndTermsModal.cancelButton.click();
        });

        step("4. Open the Select Package Tab, check Contract checkbox", () -> {
            packagePage.openTab();
            packagePage.packageSelector.setContractSelected(true);
        });

        //  CRM-27296
        step("5. Open the Price tab, click 'Billing Details and Terms' button, set Initial Term, Renewal Term, Number of Months fields, " +
                "save changes and check DQ Approver button's text", () -> {
            cartPage.openTab();
            cartPage.footer.billingDetailsAndTermsButton.click();
            cartPage.billingDetailsAndTermsModal.initialTermPicklist.selectOption(initialTerm);
            cartPage.billingDetailsAndTermsModal.renewalTermPicklist.selectOption(renewalTerm);
            cartPage.billingDetailsAndTermsModal.specialTermsPicklist.selectOptionContainingText(oneFreeMonthServiceCredit);
            cartPage.applyChangesInBillingDetailsAndTermsModal();
            cartPage.saveChanges();

            cartPage.dqApproverButton.shouldHave(exactTextCaseSensitive(NO_APPROVAL_REQUIRED_DQ_APPROVER));
        });

        //  CRM-27297
        step("6. Click 'Billing Details and Terms' button, select Number of Months picklist value that's more than Initial Terms (in years), " +
                "save changes, and check DQ Approver button's text", () -> {
            cartPage.footer.billingDetailsAndTermsButton.click();
            cartPage.billingDetailsAndTermsModal.specialTermsPicklist.selectOptionContainingText(twoFreeMonthsServiceCredit);
            cartPage.applyChangesInBillingDetailsAndTermsModal();
            cartPage.saveChanges();

            cartPage.dqApproverButton.shouldHave(exactTextCaseSensitive(FINANCE_REVENUE_DQ_APPROVER));
        });

        //  CRM-27297
        step("7. Click on the DQ Approver button, " +
                "check Approver levels in the 'View Approvers' modal window", () -> {
            cartPage.dqApproverButton.click();
            cartPage.viewDqApproversModal.approverLevels.should(containExactTextsCaseSensitive(expectedApproverLevels));
        });
    }
}
