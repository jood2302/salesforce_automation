package ngbs.quotingwizard.newbusiness.carttab.promotions;

import base.BaseTest;
import base.SfdcSteps;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.sforce.soap.enterprise.sobject.User;

import java.util.List;

import static base.Pages.cartPage;
import static base.Pages.quotePage;
import static com.aquiva.autotests.rc.model.ngbs.dto.discounts.PromotionNgbsDTO.PROMO_EXPIRED;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.createPromotionInNGBS;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.searchPromotionsByPromoCodeInNGBS;
import static com.aquiva.autotests.rc.utilities.ngbs.PromotionNgbsFactory.createPromotionForLicensesWithMasterPlanDuration;
import static com.aquiva.autotests.rc.utilities.ngbs.PromotionNgbsFactory.createPromotionForRcOfficeProduct;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.exactValue;
import static com.codeborne.selenide.Selenide.sleep;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.time.Duration.ofSeconds;

/**
 * Test methods for test cases related to the testing of the "Promotions" functionality on the Price tab.
 */
public class PromosSteps {
    private final SfdcSteps sfdcSteps;

    private User testUserWithPromosFeature;

    //  Test data
    public final Promotion[] promotions;
    public final String ngbsPackageId;
    public final String initialTerm;
    private final AreaCode localAreaCode;

    /**
     * New instance for the class with the test methods/steps for test cases
     * related to the testing of the "Promotions" functionality on the Price tab.
     *
     * @param data object parsed from the JSON files with the test data
     */
    public PromosSteps(Dataset data) {
        sfdcSteps = new SfdcSteps();

        promotions = data.packageFolders[0].packages[0].promotions;
        ngbsPackageId = data.packageFolders[0].packages[0].id;
        initialTerm = data.getInitialTerm();
        localAreaCode = new AreaCode("Local", "United States", "California", EMPTY_STRING, "619");
    }

    /**
     * Create new promotions in NGBS according to the provided test data.
     */
    public void createPromotionsInNGBS() {
        synchronized (BaseTest.class) {   //  sync threads to run tests in parallel that might create the same promo
            for (var promotionTestData : promotions) {
                step("Prepare test promotion '" + promotionTestData.promoDescription + "' in NGBS", () -> {
                    var existingPromotions = searchPromotionsByPromoCodeInNGBS(promotionTestData.promoCode);

                    if (existingPromotions.length != 0) {
                        var firstFoundPromotion = existingPromotions[0];

                        if (firstFoundPromotion.status.equalsIgnoreCase(PROMO_EXPIRED)) {
                            throw new AssertionError("Your test promotion is expired!" +
                                    "Update your test data for the promotion with a new promo code and run your test again! \n" +
                                    "Found promotion: " + firstFoundPromotion);
                        }
                    } else {
                        var newPromotion = promotionTestData.discountTemplates.length > 1 ?
                                createPromotionForLicensesWithMasterPlanDuration(promotionTestData, ngbsPackageId) :
                                createPromotionForRcOfficeProduct(promotionTestData);

                        createPromotionInNGBS(newPromotion);
                    }
                });
            }
        }
    }

    /**
     * Find a user with 'Sales Rep - Lightning' profile and enabled Promotions feature via SFDC API.
     */
    public User getTestUserWithPromosFeature() {
        return step("Find a user with 'Sales Rep - Lightning' profile and 'Enable_Promotions__c' feature toggle", () -> {
            testUserWithPromosFeature = getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    .withFeatureToggles(List.of(ENABLE_PROMOTIONS_FT))
                    .execute();

            return testUserWithPromosFeature;
        });
    }

    /**
     * Initial login as a user with 'Sales Rep - Lightning' profile and enabled Promotions feature
     * via Salesforce login page.
     */
    public void loginAsTestUserWithPromosFeature() {
        step("Login as a user with 'Sales Rep - Lightning' profile and 'Enable_Promotions__c' feature toggle", () -> {
            sfdcSteps.initLoginToSfdcAsTestUser(testUserWithPromosFeature);
        });
    }

    /**
     * Check that promotion was applied correctly for the given product.
     * Note: use it when the promotion has more than one discount template.
     *
     * @param product           product affected by the promotion
     * @param promoName         expected promotion's name
     * @param promotionDiscount expected promotion's discount values on the product after applying promotion
     */
    public void stepCheckAppliedPromo(Product product, String promoName, PromotionDiscountTemplate promotionDiscount) {
        step("Check the applied promotion for the product '" + product.name + "'", () -> {
            var productQLI = cartPage.getQliFromCartByDisplayName(product.name);
            productQLI.getDiscountInput().shouldHave(exactValue(valueOf(promotionDiscount.value)));
            productQLI.getDiscountTypeSelect().getSelectedOption().shouldHave(exactTextCaseSensitive(promotionDiscount.type));

            sleep(1_000); // to stabilize the next hover action
            productQLI.getPromoIcon().hover();

            var expectedTooltip = format("%s: %s", promoName, promotionDiscount.description);
            cartPage.tooltip.shouldHave(exactTextCaseSensitive(expectedTooltip), ofSeconds(10));
        });
    }

    /**
     * Check that promotion was applied correctly for the given product.
     * Note: use it when the promotion has only a single discount template.
     *
     * @param product   product affected by the promotion
     * @param promotion expected promotion values on the product after applying promotion
     */
    public void stepCheckAppliedPromo(Product product, Promotion promotion) {
        stepCheckAppliedPromo(product, promotion.promoName, promotion.discountTemplates[0]);
    }

    /**
     * Open the Quote Details tab, populate Main Area Code, initial term and Start Date, and save changes.
     */
    public void populateRequiredInformationOnQuoteDetailsTab() {
        quotePage.openTab();
        quotePage.setMainAreaCode(localAreaCode);
        quotePage.initialTermPicklist.selectOption(initialTerm);
        quotePage.setDefaultStartDate();
        quotePage.saveChanges();
    }
}
