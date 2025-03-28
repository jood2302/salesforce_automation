package com.aquiva.autotests.rc.utilities.ngbs;

import com.aquiva.autotests.rc.model.ngbs.dto.discounts.DiscountNgbsDTO;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;

import java.time.Clock;

import static com.aquiva.autotests.rc.model.ngbs.dto.discounts.DiscountNgbsDTO.ApplicableTo.ApplicableType.CATALOG_ID;
import static com.aquiva.autotests.rc.model.ngbs.dto.discounts.DiscountNgbsDTO.DiscountValue.getDiscountValueForSerialization;
import static java.lang.String.format;

/**
 * Factory for generating instances of {@link DiscountNgbsDTO} objects.
 * <br/><br/>
 * Note: priority for Discount Template Group should be unique across all the discounts on the NGBS account.
 * Therefore, it's necessary to keep the state for every running test.
 * That's why it's required to create <i>a single instance of the factory per test</i>
 * in order to use its methods for discount creation.
 */
public class DiscountNgbsFactory {
    private static final char PRIORITY_DEFAULT = 'A';

    private int priorityCounter = 0;

    /**
     * Get the next available priority value for Discount object.
     * NGBS REST API requires discount templates with unique priorities within one account entity.
     * And API accepts values from "A" to "Z" (from lowest to highest priority).
     * <br/><br/>
     * <i> Note: used in methods that construct Discount DTO for sending as JSON to NGBS API. </i>
     *
     * @return priority values starting from default priority in ascending order
     * (from "A" to "B" to "C"... to "Z" and then again, from "A" to "B"...)
     */
    public String getNextAvailablePriority() {
        if (priorityCounter >= 26) {
            priorityCounter = 0;
        }
        return String.valueOf((char) (PRIORITY_DEFAULT + priorityCounter++));
    }

    /**
     * Create a discount "object" for selected license using provided test data product.
     * Such object could be used later in NGBS REST request
     * for creating discount on existing account.
     * <br/>
     * Note: a discount will be created for the current package on the NGBS account.
     *
     * @param productWithDiscount test data for a product to create a discount for
     *                            (must have non-null {@code dataName, discount, discountType, chargeTerm}
     *                            variables)
     * @return Discount DTO object to pass on in NGBS REST API request methods.
     */
    public DiscountNgbsDTO createDiscountTemplateGroup(Product productWithDiscount) {
        return createDiscountTemplateGroup(
                null,
                new DiscountNgbsDTO.ApplicableTo(productWithDiscount.dataName, CATALOG_ID),
                productWithDiscount.discount.doubleValue(),
                productWithDiscount.discountType,
                productWithDiscount.chargeTerm);
    }

    /**
     * Create a discount "object" for selected license using provided discount values.
     * Such object could be used later in NGBS REST request
     * for creating discount on existing account.
     *
     * @param target               object with package information (id and version)
     *                             (e.g. {catalogId="18",version="3"} for RingCentral MVP Standard - v.3;
     *                             {catalogId="6",version="1"} for RingCentral Meetings Free - v.1, etc...)
     *                             <br/><br/>
     * @param applicableTo         data object to show where exactly this discount applies
     *                             <p> Examples: </p>
     *                             <p> ApplicableTo{id="LC_DL-UNL_50",type="CatalogId"} for discount on DigitalLine Unlimited product; </p>
     *                             <p> ApplicableTo{id="4050457005",type="LicenseId"} for discount on one of the specific items; </p>
     *                             <p> ApplicableTo{id="IBO",type="CategoryId"} for discount on entire 'IBO' category; </p>
     *                             <br/><br/>
     * @param discountAmount       discount numeric value
     *                             (e.g. 42, 7.35, etc...)
     *                             <br/><br/>
     * @param discountType         discount type value
     *                             (e.g. "%" for percents, or "USD", "EUR", etc for currencies)
     *                             <br/><br/>
     * @param chargeTerm           charge term for payments
     *                             (e.g. "Monthly", "Annual", etc...)
     * @return Discount DTO object to pass on in NGBS REST API request methods.
     */
    public DiscountNgbsDTO createDiscountTemplateGroup(
            DiscountNgbsDTO.Target target,
            DiscountNgbsDTO.ApplicableTo applicableTo,
            Double discountAmount,
            String discountType,
            String chargeTerm) {
        var discountTemplateGroup = new DiscountNgbsDTO();

        discountTemplateGroup.description = "QA Auto Test";
        discountTemplateGroup.priority = getNextAvailablePriority();
        discountTemplateGroup.visible = true;

        var systemDateTimeUTCNow = Clock.systemUTC().instant().toString();
        discountTemplateGroup.addedAt = systemDateTimeUTCNow;

        discountTemplateGroup.target = target;

        var discountTemplate = new DiscountNgbsDTO.DiscountTemplate();
        discountTemplate.description = format("%s %s off to %s='%s' (QA Auto)",
                discountAmount, discountType, applicableTo.getType(), applicableTo.getId());

        var applicationTerm = new DiscountNgbsDTO.ApplicationTerm();
        applicationTerm.setLicenseScope(applicableTo.getType());
        applicationTerm.startDate = systemDateTimeUTCNow;
        discountTemplate.applicationTerms = applicationTerm;

        discountTemplate.applicableTo = applicableTo;

        var value = new DiscountNgbsDTO.Value();
        var discountValue = getDiscountValueForSerialization(discountType, discountAmount);
        value.mapChargeTermToDiscount(chargeTerm, discountValue);
        discountTemplate.values = value;

        discountTemplateGroup.discountTemplates = new DiscountNgbsDTO.DiscountTemplate[]{discountTemplate};

        return discountTemplateGroup;
    }
}
