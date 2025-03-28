package com.aquiva.autotests.rc.utilities.ngbs;

import com.aquiva.autotests.rc.model.ngbs.dto.discounts.DiscountNgbsDTO;
import com.aquiva.autotests.rc.model.ngbs.dto.discounts.PromotionNgbsDTO;
import com.aquiva.autotests.rc.model.ngbs.testdata.Promotion;
import com.aquiva.autotests.rc.model.ngbs.testdata.PromotionDiscountTemplate;

import static com.aquiva.autotests.rc.model.ngbs.dto.discounts.DiscountNgbsDTO.DiscountValue.getDiscountValueForSerialization;
import static com.aquiva.autotests.rc.model.ngbs.dto.discounts.PromotionNgbsDTO.PromoAvailability.AvailableType.PACKAGE;
import static com.aquiva.autotests.rc.model.ngbs.dto.discounts.PromotionNgbsDTO.PromoAvailability.AvailableType.PRODUCT;

/**
 * Factory for generating instances of {@link PromotionNgbsDTO} objects.
 */
public class PromotionNgbsFactory {

    /**
     * Create Promotion object using its corresponding test data object.
     * Used for promotion for products of RC Office packages.
     *
     * @param promotionTestData promotion object with test data (promo code, discount value, etc...)
     * @return promotion DTO to pass on in NGBS REST API request methods.
     */
    public static PromotionNgbsDTO createPromotionForRcOfficeProduct(Promotion promotionTestData) {
        var promo = new PromotionNgbsDTO(promotionTestData);

        var productAvailabilityType = new PromotionNgbsDTO.PromoAvailability(null, PRODUCT, "2");
        promo.availability = new PromotionNgbsDTO.PromoAvailability[]{productAvailabilityType};

        var discountTemplate = createDiscountTemplateForPromotion(
                promotionTestData.discountTemplates[0], promotionTestData.chargeTerm);
        promo.discountTemplates = new DiscountNgbsDTO.DiscountTemplate[]{discountTemplate};

        return promo;
    }

    /**
     * Create Promotion object using its corresponding test data object.
     * Used for promotions that apply to the pair of "parent/child" products of the certain package.
     *
     * @param promotionTestData promotion object with test data for both products:
     *                          (promo code, discount data for parent and child products, etc...)
     * @param packageId         id of package for what promo will be available
     *                          (e.g. "592" for Engage Digital Standalone package)
     * @return promotion DTO to pass on in NGBS REST API request methods.
     */
    public static PromotionNgbsDTO createPromotionForLicensesWithMasterPlanDuration(Promotion promotionTestData,
                                                                                    String packageId) {
        var promo = new PromotionNgbsDTO(promotionTestData);

        var productAvailabilityType = new PromotionNgbsDTO.PromoAvailability(null, PACKAGE, packageId);
        promo.availability = new PromotionNgbsDTO.PromoAvailability[]{productAvailabilityType};

        var parentDiscountTemplate = createDiscountTemplateForPromotion(
                promotionTestData.discountTemplates[0], promotionTestData.chargeTerm);
        parentDiscountTemplate.valueTerm = DiscountNgbsDTO.DiscountTemplate.LICENSE_DURATION_VALUE_TERM;

        var childDiscountTemplate = createDiscountTemplateForPromotion(
                promotionTestData.discountTemplates[1], promotionTestData.chargeTerm);
        childDiscountTemplate.valueTerm = DiscountNgbsDTO.DiscountTemplate.PACKAGE_MASTER_DURATION_VALUE_TERM;

        promo.discountTemplates = new DiscountNgbsDTO.DiscountTemplate[]{parentDiscountTemplate, childDiscountTemplate};

        return promo;
    }

    /**
     * Create discount template for provided item, which will be used in promotion.
     *
     * @param promoDiscountData promotion's discount data
     * @param chargeTerm        charge term of provided License
     * @return discount template object to set up for Promotion DTO
     */
    private static DiscountNgbsDTO.DiscountTemplate createDiscountTemplateForPromotion(PromotionDiscountTemplate promoDiscountData,
                                                                                       String chargeTerm) {
        var discountTemplate = new DiscountNgbsDTO.DiscountTemplate();

        discountTemplate.name = promoDiscountData.name;
        discountTemplate.description = promoDiscountData.description;
        var applicationTerm = new DiscountNgbsDTO.ApplicationTerm();
        applicationTerm.setLicenseScope(promoDiscountData.applicableTo.getType());
        discountTemplate.applicationTerms = applicationTerm;

        discountTemplate.applicableTo = promoDiscountData.applicableTo;

        var value = new DiscountNgbsDTO.Value();
        var discountValueTemplate =
                getDiscountValueForSerialization(promoDiscountData.type, promoDiscountData.value.doubleValue());
        value.mapChargeTermToDiscount(chargeTerm, discountValueTemplate);
        discountTemplate.values = value;

        return discountTemplate;
    }
}
