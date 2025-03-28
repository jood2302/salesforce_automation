package ngbs.quotingwizard.newbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.cartPage;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartPage.REQUIRED_APPROVAL_STATUS;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("NGBS")
public class DiscountApprovalTest extends BaseTest {
    private final Steps steps;

    //  Test data
    private final Product digitalLineUnlimited;

    public DiscountApprovalTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_NonContract_1TypeOfDL.json",
                Dataset.class);
        steps = new Steps(data);

        digitalLineUnlimited = data.getProductByDataName("LC_DL-UNL_50");
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-7854")
    @DisplayName("CRM-7854 - New Business. GOA On the Price tab")
    @Description("Verify that discounts over a certain threshold require approval")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "and select a package for it", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.selectDefaultPackageFromTestData();
        });

        step("2. Open the Price tab, add discount above the threshold to the product, save changes, " +
                "and check the Approval Status value", () -> {
            cartPage.openTab();
            cartPage.setDiscountTypeForQLItem(digitalLineUnlimited.name, digitalLineUnlimited.discountType);
            cartPage.setDiscountForQLItem(digitalLineUnlimited.name, digitalLineUnlimited.newDiscount);
            cartPage.saveChanges();

            cartPage.approvalStatus.shouldHave(exactTextCaseSensitive(REQUIRED_APPROVAL_STATUS));
        });
    }
}
