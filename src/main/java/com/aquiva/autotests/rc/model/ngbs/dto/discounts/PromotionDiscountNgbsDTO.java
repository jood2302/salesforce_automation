package com.aquiva.autotests.rc.model.ngbs.dto.discounts;

import com.aquiva.autotests.rc.model.DataModel;
import com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient;
import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Data object with promotion information for usage with NGBS API services.
 * <br/><br/>
 * Useful data structure for creating promotion request objects
 * in order to add promotion discount to existing accounts in NGBS.
 *
 * @see NgbsRestApiClient#createPromoDiscountInNGBS
 */
@JsonInclude(value = NON_NULL)
public class PromotionDiscountNgbsDTO extends DataModel {
    public String code;
    public DiscountNgbsDTO.Target target;

    /**
     * Default no-arg constructor.
     * It's needed for data serialization with Jackson data mapper.
     */
    public PromotionDiscountNgbsDTO() {
    }

    /**
     * Create a new instance of promotion discount data on the NGBS account.
     *
     * @param code   a promo code from the existing promotion (e.g. "QA-AUTO-DLBASIC-LICENSE-USD-11-24")
     * @param target object with package information (id and version)
     *               (e.g. {catalogId="1231005",version="2"} for RingEX Core™ - v.2;
     *               {catalogId="666",version="1"} for RingCentral Meetings Video Pro - v.1, etc...)
     */
    public PromotionDiscountNgbsDTO(String code, DiscountNgbsDTO.Target target) {
        this.code = code;
        this.target = target;
    }
}
