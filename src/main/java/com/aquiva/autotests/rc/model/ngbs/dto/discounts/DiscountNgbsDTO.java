package com.aquiva.autotests.rc.model.ngbs.dto.discounts;

import com.aquiva.autotests.rc.model.DataModel;
import com.aquiva.autotests.rc.utilities.StringHelper;
import com.aquiva.autotests.rc.utilities.ngbs.DiscountNgbsFactory;
import com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient;
import com.fasterxml.jackson.annotation.*;

import static com.aquiva.autotests.rc.model.ngbs.dto.discounts.DiscountNgbsDTO.ApplicableTo.ApplicableType.LICENSE_ID;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Data object with discount information for usage with NGBS API services.
 * <br/><br/>
 * Useful data structure for creating discount request objects and parsing responses
 * to/from NGBS API discount service (see {@link NgbsRestApiClient} for a reference).
 * <br/><br/>
 * Use {@link DiscountNgbsFactory} to create quick instances of this DTO.
 */
@JsonInclude(value = NON_NULL)
public class DiscountNgbsDTO extends DataModel {
    public String id;
    public String packageId;
    public String contractId;
    public String description;
    public String priority;
    public String addedAt;
    public String promoCode;
    public String promoType;
    public Boolean visible;
    public Target target;
    public DiscountTemplate[] discountTemplates;

    //  <editor-fold defaultstate="collapsed" desc="Inner structure classes">

    /**
     * Inner data structure for Discount data object.
     * Represents data for account's package.
     */
    @JsonInclude(value = NON_NULL)
    public static class Target extends DataModel {
        public String catalogId;

        public String version;

        /**
         * Constructor with parameters.
         * Apart from usual purpose, it's used by Jackson deserializer
         * to construct object from JSON response from NGBS API.
         *
         * @param catalogId unique package ID for account's package
         *                  (e.g. "18" for RingCentral MVP Standard;
         *                  "6" for RingCentral Meetings Free, etc...)
         *                  <p></p>
         * @param version   package version for account's package
         *                  (usually "1", but sometimes may be "2" or "3" or else)
         *                  <p></p>
         */
        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public Target(@JsonProperty("catalogId") String catalogId, @JsonProperty("version") String version) {
            this.catalogId = catalogId;
            this.version = version;
        }
    }

    /**
     * Inner data structure for Discount data object.
     * Represents data for discount terms (charge term, amount, license...)
     */
    @JsonInclude(value = NON_NULL)
    public static class DiscountTemplate extends DataModel {
        //  For 'valueTerm'
        public static final String LICENSE_DURATION_VALUE_TERM = "LicenseDuration";
        public static final String PACKAGE_MASTER_DURATION_VALUE_TERM = "PackageMasterDuration";

        public String id;
        public String name; //  used only for discounts in promotions
        public String description;
        public ApplicableTo applicableTo;
        public ApplicationTerm applicationTerms;
        public Value values;
        public String valueTerm;
    }

    /**
     * Inner data structure for DiscountTemplate data object.
     * Represents data for specific discounted license.
     */
    @JsonInclude(value = NON_NULL)
    public static class ApplicableTo extends DataModel {

        /**
         * Constants for ApplicableTo type variable.
         */
        public enum ApplicableType {
            /**
             * For discounts on product entity.
             */
            CATALOG_ID("CatalogId"),
            /**
             * For discounts on entire product categories.
             */
            CATEGORY_ID("CategoryId"),
            /**
             * For discounts on exact licenses.
             */
            LICENSE_ID("LicenseId");

            private final String typeValue;

            ApplicableType(String typeValue) {
                this.typeValue = typeValue;
            }
        }

        private final String id;

        private final String type;

        /**
         * Special constructor for Jackson deserializer.
         * Helps with correct mapping for a class with constructor with parameters,
         * that aren't entirely aligned with class instance variables.
         *
         * @param id   some form ID to apply discount to
         * @param type type of discount (see class constants for reference}
         */
        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        private ApplicableTo(@JsonProperty("id") String id, @JsonProperty("type") String type) {
            this.id = id;
            this.type = type;
        }

        /**
         * Constructor for ApplicableTo object.
         * Objects indicates where exactly the discount applies.
         * <p> Examples: </p>
         * <p> - ApplicableTo{id="LC_DL-UNL_50",type="CatalogId"} for discount on DigitalLine Unlimited product; </p>
         * <p> - ApplicableTo{id="4050457005",type="LicenseId"} for discount on one of the specific items; </p>
         * <p> - ApplicableTo{id="IBO",type="CategoryId"} for discount on entire 'IBO' product category; </p>
         *
         * <p></p>
         * <i> Note: used in methods that construct Discount DTO for sending as JSON to NGBS API. </i>
         *
         * @param id   some form ID to apply discount to
         * @param type type of discount (see class constants for reference}
         */
        public ApplicableTo(String id, ApplicableType type) {
            this(id, type.typeValue);
        }

        /**
         * Get discount ID. Depends on {@link ApplicableTo#getType()}  parameter.
         *
         * @return discount template group identifier (e.g. "LC_DL-UNL_50", "4050457005"... )
         */
        public String getId() {
            return id;
        }

        /**
         * Get type of discount.
         *
         * @return discount template group type (see {@link ApplicableType})
         */
        public String getType() {
            return type;
        }
    }

    /**
     * Inner data structure for DiscountTemplate data object.
     * Represents data for discount's term (start/end date, etc...)
     */
    @JsonInclude(value = NON_NULL)
    public static class ApplicationTerm extends DataModel {
        public static final String SCOPE_EXACT = "Exact";

        public static final String SCOPE_ALL = "All";
        public String startDate;
        public String endDate;
        public String maxLicenses;

        public String licenseScope;

        /**
         * Set licenseScope variable with additional validation.
         * <p></p>
         * <i> Note: used in methods that construct Discount DTO for sending as JSON to NGBS API. </i>
         *
         * @param type type of discount (e.g. "CatalogId")
         */
        public void setLicenseScope(String type) {
            this.licenseScope = type.equals(LICENSE_ID.typeValue) ?
                    SCOPE_EXACT :
                    SCOPE_ALL;
        }
    }

    /**
     * Inner data structure for DiscountTemplate data object.
     * Represents mapping data for discount's charge term.
     */
    @JsonInclude(value = NON_NULL)
    public static class Value extends DataModel {

        @JsonProperty("Monthly")
        public DiscountValue monthly;

        @JsonProperty("Annual")
        public DiscountValue annual;

        @JsonProperty("OneTime")
        public DiscountValue oneTime;

        @JsonProperty("Quarterly")
        public DiscountValue quarterly;

        @JsonProperty("SemiAnnual")
        public DiscountValue semiAnnual;

        /**
         * Map charge term parameter with discount value structure.
         * <p></p>
         * <i> Note: used in methods that construct Discount DTO for sending as JSON to NGBS API. </i>
         *
         * @param chargeTerm    charge term value (e.g. "Monthly")
         * @param discountValue {@link DiscountValue} object with discount type and value
         */
        public void mapChargeTermToDiscount(String chargeTerm, DiscountValue discountValue) {
            switch (chargeTerm.toUpperCase().replaceAll("\\s", "")) {
                default:
                case "MONTHLY":
                    this.monthly = discountValue;
                    break;
                case "ANNUAL":
                    this.annual = discountValue;
                    break;
                case "ONE-TIME":
                    this.oneTime = discountValue;
                    break;
                case "QUARTERLY":
                    this.quarterly = discountValue;
                    break;
                case "SEMIANNUAL":
                    this.semiAnnual = discountValue;
                    break;
            }
        }
    }

    /**
     * Inner data structure for Value data object.
     * Represents data with discount type and value.
     */
    @JsonInclude(value = NON_NULL)
    public static class DiscountValue extends DataModel {
        public String unit;

        public Double value;

        /**
         * Constructor with parameters.
         * This constructor is used by Jackson deserializer, to parse JSON responses from NGBS API.
         *
         * @param unit  discount type value
         *              (e.g. "Percent" or "Money")
         *              <p></p>
         * @param value discount numeric value
         *              (e.g. 42, 7.35, etc...)
         *              <p></p>
         */
        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public DiscountValue(@JsonProperty("unit") String unit, @JsonProperty("value") Double value) {
            this.unit = unit;
            this.value = value;
        }

        /**
         * Get DiscountValue object for further serialization.
         * <p></p>
         * <i> Note: used in methods that construct Discount DTO for sending as JSON to NGBS API. </i>
         *
         * @param unit  discount type value
         *              (e.g. "%" for percents, or "USD", "EUR", etc for currencies)
         *              <p></p>
         * @param value discount numeric value
         *              (e.g. 42, 7.35, etc...)
         *              <p></p>
         * @return {@link DiscountValue} object for further serialization.
         */
        public static DiscountValue getDiscountValueForSerialization(String unit, Double value) {
            var unitForSerialization = unit.equals(StringHelper.PERCENT) ? "Percent" : "Money";
            return new DiscountValue(unitForSerialization, value);
        }
    }

    //  </editor-fold>

}
