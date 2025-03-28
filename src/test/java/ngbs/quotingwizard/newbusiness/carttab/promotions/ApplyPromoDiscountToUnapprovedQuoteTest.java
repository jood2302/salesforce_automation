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
public class ApplyPromoDiscountToUnapprovedQuoteTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final PromosSteps promosSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final Promotion recurringUsdDiscountPromo;
    private final Promotion crfPercentDiscountPromo;
    private final Product dlUnlimited;
    private final Product polycomPhone;
    private final Product complianceRecoveryFee;

    public ApplyPromoDiscountToUnapprovedQuoteTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_AnnualAndMonthly_Contract_PhonesAndDLs_Promos.json",
                Dataset.class);

        steps = new Steps(data);
        promosSteps = new PromosSteps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        recurringUsdDiscountPromo = promosSteps.promotions[0];
        crfPercentDiscountPromo = promosSteps.promotions[5];
        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        polycomPhone = data.getProductByDataName("LC_HD_936");

        //  this value should exceed a 'ceiling' configured in the custom metadata types: 
        //  DiscountCeilingCBox__mdt, DiscountCeilingCategory__mdt, DiscountCeilingLicense__mdt
        polycomPhone.discount = 81;

        complianceRecoveryFee = data.getProductByDataName("LC_CRF_51");
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
    @TmsLink("CRM-21781")
    @DisplayName("CRM-21781 - Applying Promo code to the unapproved Quote with applied flexible discounts")
    @Description("To verify that Promo code can be applied when Quote already contains some flexible discounts and requires approval")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, save changes, and add some products on the Add Products tab", () -> {
            steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addProductsOnProductsTab(data.getNewProductsToAdd());
        });

        step("2. Open the Price tab, apply any Promo code and check that it's applied correctly", () -> {
            cartPage.openTab();
            cartPage.applyPromoCode(recurringUsdDiscountPromo.promoCode);

            promosSteps.stepCheckAppliedPromo(dlUnlimited, recurringUsdDiscountPromo);
        });

        step("3. Add flexible discount to the any item not in promotion, save changes, " +
                "and check the Quote Approval Status", () -> {
            cartPage.setDiscountForQLItem(polycomPhone.name, polycomPhone.discount);
            cartPage.saveChanges();

            cartPage.approvalStatus.shouldHave(exactTextCaseSensitive(REQUIRED_APPROVAL_STATUS));
        });

        step("4. Remove old promo, apply any promo that doesn't override existing flexible discount, save changes, " +
                "and check the Quote Approval Status", () -> {
            cartPage.openTab();
            cartPage.changeAppliedPromo(crfPercentDiscountPromo.promoCode);

            promosSteps.stepCheckAppliedPromo(complianceRecoveryFee, crfPercentDiscountPromo);
            cartPage.saveChanges();

            cartPage.approvalStatus.shouldHave(exactTextCaseSensitive(REQUIRED_APPROVAL_STATUS));
        });

        step("5. Open the Quote Details tab, populate Main Area Code, Initial Term and Start Date, " +
                "save changes and set it to Active Agreement via API", () -> {
            promosSteps.populateRequiredInformationOnQuoteDetailsTab();
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
