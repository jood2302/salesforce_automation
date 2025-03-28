package ngbs.quotingwizard.engage;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import ngbs.quotingwizard.newbusiness.carttab.promotions.PromosSteps;
import org.junit.jupiter.api.*;

import static base.Pages.cartPage;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("Promos")
public class EngageUsageLicensePromoTest extends BaseTest {
    private final Steps steps;
    private final PromosSteps promosSteps;

    //  Test data
    private final Promotion promotion;
    private final Product engageSeats;
    private final Product engageSeatsOnDemand;

    public EngageUsageLicensePromoTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_EngageDS_Annual_WithPromo.json",
                Dataset.class);

        steps = new Steps(data);
        promosSteps = new PromosSteps(data);

        promotion = promosSteps.promotions[0];
        engageSeats = data.getProductByDataName("SA_SEAT_5");
        engageSeatsOnDemand = data.getProductByDataName("SA_SEATO_6");
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
    @TmsLink("CRM-25322")
    @TmsLink("CRM-22508")
    @DisplayName("CRM-25322 - Applying promo with discount for the Usage license. \n" +
            "CRM-22508 - Visibility and work of Promos button for Engage Standalone opportunities")
    @Description("CRM-25322 - Verify that discount is not applied to the usage license if its master charge term doesn't match package charge term. \n" +
            "Verify that discount is applied to the usage license if it's master charge term matches package charge term. \n" +
            "CRM-22508 - To verify that the 'Promos' button is displayed on a quote of Engage Opportunity and Promo can be applied")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, save changes and open the Price tab", () -> {
            steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId());
            cartPage.openTab();
        });

        step("2. Apply Promo code and verify that discounts are applied to products", () -> {
            //  For CRM-22508
            cartPage.applyPromoCode(promotion.promoCode);

            //  For CRM-25322
            var parentDiscountTemplateData = promotion.discountTemplates[0];
            promosSteps.stepCheckAppliedPromo(engageSeats, promotion.promoName, parentDiscountTemplateData);

            var childDiscountTemplateData = promotion.discountTemplates[1];
            promosSteps.stepCheckAppliedPromo(engageSeatsOnDemand, promotion.promoName, childDiscountTemplateData);
        });
    }
}
