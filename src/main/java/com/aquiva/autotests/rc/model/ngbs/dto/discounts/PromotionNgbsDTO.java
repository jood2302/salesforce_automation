package com.aquiva.autotests.rc.model.ngbs.dto.discounts;

import com.aquiva.autotests.rc.model.DataModel;
import com.aquiva.autotests.rc.model.ngbs.testdata.Promotion;
import com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient;
import com.aquiva.autotests.rc.utilities.ngbs.PromotionNgbsFactory;
import com.fasterxml.jackson.annotation.*;

import java.time.Clock;

import static com.aquiva.autotests.rc.model.ngbs.dto.discounts.PromotionNgbsDTO.PromoLabels.AccessLabels.SFDC_DIRECT;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Data object with promotion information for usage with NGBS API services.
 * <br/><br/>
 * Useful data structure for creating promotion request objects and parsing responses
 * to/from NGBS API discount service (see {@link NgbsRestApiClient} for a reference).
 * <br/><br/>
 * Use {@link PromotionNgbsFactory} to create quick instances of this DTO.
 */
public class PromotionNgbsDTO extends DiscountNgbsDTO {
    //  Constant for 'status'
    public static final String PROMO_EXPIRED = "Expired";

    public String id;
    public String name;
    public String description;
    public String promoCode;
    public String startDate;
    public String endDate;
    public String priority;
    public PromoAvailability[] availability;
    public Labels labels;
    public String status;
    public PromoCodeType promoType;
    public String goa;

    public DiscountTemplate[] discountTemplates;

    /**
     * Create a promotion object with some default values from test data object.
     * Used for regular promotions with "Direct Sales RC Sales" label.
     *
     * @param promotionTestData test data object with promotion's details (name, description, etc...)
     */
    public PromotionNgbsDTO(Promotion promotionTestData) {
        name = promotionTestData.promoName;
        promoCode = promotionTestData.promoCode;
        description = promotionTestData.promoDescription;
        startDate = Clock.systemUTC().instant().toString();
        priority = "Z";

        var sfdcDirectLabel = new PromotionNgbsDTO.Labels();
        sfdcDirectLabel.accessLabel = new PromotionNgbsDTO.PromoLabels.AccessLabels[]{SFDC_DIRECT};
        labels = sfdcDirectLabel;

        promoType = new PromotionNgbsDTO.PromoCodeType(1);
    }

    /**
     * Default no-arg constructor.
     * It's needed for data serialization with Jackson data mapper.
     */
    public PromotionNgbsDTO() {
    }

    /**
     * Inner data structure for Promotion data object.
     * Represents data for types of entities that the promotion is available for:
     * products for specific brands; products for individual opportunities;
     * products for specific packages, etc...
     */
    @JsonInclude(value = NON_NULL)
    public static class PromoAvailability extends DataModel {

        public enum AvailableType {
            PRODUCT("Product"),
            PRODUCT_LINE("Product Line"),
            EDITION("Edition"),
            PACKAGE("Package"),
            OPPORTUNITY("Opportunity");

            private final String type;

            AvailableType(String type) {
                this.type = type;
            }
        }

        public final String id;
        public final String type;
        public final String availableTo;

        /**
         * Constructor with parameters.
         * It's used by Jackson deserializer
         * to construct object from JSON response from NGBS API.
         *
         * @param id          unique availability ID for account's chargeTerm
         *                    (e.g. 18789001)
         * @param type        special type to which discount group is available
         *                    (e.g. "Product")
         * @param availableTo Id of entity for which discount group is available
         *                    (e.g., "2" for type="Product" which means all products from all Office packages
         *                    or "3" for type="Product" which means all products from all Meetings packages
         *                    or "592" for type="Package" which means all products of Engage Digital Standalone package)
         */
        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public PromoAvailability(@JsonProperty("id") String id,
                                 @JsonProperty("type") String type,
                                 @JsonProperty("availableTo") String availableTo) {
            this.id = id;
            this.type = type;
            this.availableTo = availableTo;
        }

        /**
         * Constructor with parameters.
         * Use this constructor in object's factories for consistency.
         *
         * @param id          unique availability ID for account's chargeTerm
         *                    (e.g. 18789001)
         * @param type        special enum type to which discount group is available
         *                    (e.g. PRODUCT, PACKAGE)
         * @param availableTo Id of entity for which discount group is available
         *                    (e.g., "2" for type=PRODUCT which means all products from all Office packages
         *                    or "3" for type=PRODUCT which means all products from all Meetings packages
         *                    or "592" for type=PACKAGE which means all products of Engage Digital Standalone package)
         */
        public PromoAvailability(String id,
                                 PromoAvailability.AvailableType type,
                                 String availableTo) {
            this(id, type.type, availableTo);
        }
    }

    /**
     * Inner data structure for Promotion data object.
     * Represents data for promo labels that is used
     * to map brands (like "RingCentral", "Avaya Cloud Office")
     * with the given promotion.
     */
    @JsonInclude(value = NON_NULL)
    public static class Labels extends DataModel {
        public PromoLabels.AccessLabels[] accessLabel;
    }

    /**
     * Inner data structure for Promotion's Labels data object.
     *
     * @see Labels
     */
    @JsonInclude(value = NON_NULL)
    public static class PromoLabels extends DataModel {

        public enum AccessLabels {
            /**
             * Direct Sales RC Sales.
             */
            SFDC_DIRECT("SFDC_DIRECT"),
            /**
             * Value Added Resellers.
             */
            SFDC_VAR("SFDC_VAR"),
            /**
             * Avaya Cloud Office.
             */
            SFDC_ACO("SFDC_ACO"),
            /**
             * Reseller in DE.
             */
            SFDC_ATOS("SFDC_ATOS"),
            /**
             * Website RC.com
             */
            ECOM_DIRECT("ECOM_DIRECT");

            private final String accessLabel;

            AccessLabels(String accessLabel) {
                this.accessLabel = accessLabel;
            }
        }
    }

    /**
     * Inner data structure for Promotion data object.
     * Represents data for types of promo codes on the promotion.
     * (one-time or regular).
     */
    @JsonInclude(value = NON_NULL)
    public static class PromoCodeType extends DataModel {

        public enum PromoType {
            /**
             * Regular promo that can be applied multiple times (for id = 1)
             */
            REGULAR("Regular Promo"),
            /**
             * One-Time promo that can be applied only once (for id = 2)
             */
            ONE_TIME("One-Time Promo");

            private final String typeValue;

            PromoType(String typeValue) {
                this.typeValue = typeValue;
            }
        }

        public final Integer id;
        public final String type;

        /**
         * Constructor with parameters.
         * It's used by Jackson deserializer
         * to construct object from JSON response from NGBS API.
         *
         * @param id   special Id that indicates the type of promo code
         *             (e.g. 1 - for Regular Promo; 2 - for One-Time Promo)
         * @param type type of promo code
         *             (e.g. "Regular Promo")
         */
        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public PromoCodeType(@JsonProperty("id") Integer id, @JsonProperty("type") String type) {
            this.id = id;
            this.type = type;
        }

        /**
         * Main constructor to be used in the object's factory.
         * Note: you don't need to provide "type" in API request, "id" is enough!
         * "type" will be populated when deserializing response from NGBS.
         *
         * @param id special Id that indicates the type of promo code
         *           (e.g. 1 - for Regular Promo; 2 - for One-Time Promo)
         */
        public PromoCodeType(Integer id) {
            this.id = id;
            this.type = null;
        }
    }
}
