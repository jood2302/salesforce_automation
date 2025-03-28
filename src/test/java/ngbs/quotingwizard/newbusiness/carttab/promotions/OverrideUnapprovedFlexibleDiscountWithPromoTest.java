package ngbs.quotingwizard.newbusiness.carttab.promotions;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Opportunity;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.cartPage;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartPage.REQUIRED_APPROVAL_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.CLOSED_WON_STAGE;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Tag("P1")
@Tag("Promos")
@Tag("PriceTab")
public class OverrideUnapprovedFlexibleDiscountWithPromoTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final PromosSteps promosSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final Promotion phonesUsdDiscountPromo;
    private final Product polycomPhone;

    public OverrideUnapprovedFlexibleDiscountWithPromoTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_AnnualAndMonthly_Contract_PhonesAndDLs_Promos.json",
                Dataset.class);

        steps = new Steps(data);
        promosSteps = new PromosSteps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        phonesUsdDiscountPromo = promosSteps.promotions[4];
        polycomPhone = data.getProductByDataName("LC_HD_936");

        //  this value should exceed a 'ceiling' configured in the custom metadata types: 
        //  DiscountCeilingCBox__mdt, DiscountCeilingCategory__mdt, DiscountCeilingLicense__mdt
        polycomPhone.discount = 81;
    }

    @BeforeEach
    public void setUpTest() {
        promosSteps.createPromotionsInNGBS();

        var testUserWithPromosFeature = promosSteps.getTestUserWithPromosFeature();
        steps.salesFlow.createAccountWithContactAndContactRole(testUserWithPromosFeature);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, testUserWithPromosFeature);
        promosSteps.loginAsTestUserWithPromosFeature();
    }

    @Test
    @TmsLink("CRM-21789")
    @DisplayName("CRM-21789 - Override unapproved flexible discount with Promo discount")
    @Description("To verify that unapproved flexible discount can be overridden with Promo discount. " +
            "Approval should be cancelled in this case")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, and add some products on the Add Products tab", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.selectDefaultPackageFromTestData();
            steps.quoteWizard.addProductsOnProductsTab(data.getNewProductsToAdd());
        });

        step("2. Open the Price tab, add flexible discount to trigger the quote's approval process, save changes, " +
                "and check the Quote Approval Status", () -> {
            cartPage.openTab();
            cartPage.setDiscountForQLItem(polycomPhone.name, polycomPhone.discount);
            cartPage.saveChanges();

            cartPage.approvalStatus.shouldHave(exactTextCaseSensitive(REQUIRED_APPROVAL_STATUS));
        });

        step("3. Apply promo which overrides unapproved flexible discount", () -> {
            cartPage.applyPromoCode(phonesUsdDiscountPromo.promoCode);

            promosSteps.stepCheckAppliedPromo(polycomPhone, phonesUsdDiscountPromo);
        });

        step("4. Open the Quote Details tab, populate Main Area Code, Initial Term and Start Date, " +
                "save changes, open the Price tab, and check the Quote Approval Status", () -> {
            promosSteps.populateRequiredInformationOnQuoteDetailsTab();

            cartPage.openTab();
            cartPage.approvalStatus.shouldNotHave(exactTextCaseSensitive(REQUIRED_APPROVAL_STATUS));
        });

        step("5. Update the Quote to Active Agreement via API", () -> {
            steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(steps.quoteWizard.opportunity);
        });

        step("6. Close the Opportunity via API, and check its 'StageName' and 'IsClosed' fields' values in DB", () -> {
            steps.quoteWizard.stepCloseOpportunity(steps.quoteWizard.opportunity);

            var opportunityClosed = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, IsClosed, StageName " +
                            "FROM Opportunity " +
                            "WHERE Id = '" + steps.quoteWizard.opportunity.getId() + "'",
                    Opportunity.class);
            assertThat(opportunityClosed.getStageName())
                    .as("Opportunity.StageName value")
                    .isEqualTo(CLOSED_WON_STAGE);
            assertThat(opportunityClosed.getIsClosed())
                    .as("Opportunity.IsClosed value")
                    .isTrue();
        });
    }
}
