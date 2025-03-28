package com.aquiva.autotests.rc.model.ngbs.testdata;

import com.aquiva.autotests.rc.model.DataModel;
import com.aquiva.autotests.rc.model.ngbs.dto.discounts.DiscountNgbsDTO;

/**
 * Test data object that represents a discount data for a specific promotion.
 * It is used to contain a test data for actual test actions
 * (changing the state of SUT; assertions; etc...)
 * <br/><br/>
 * Normally, this object consists of:
 * <p> - discount name (e.g. "Category: DL unlimited") </p>
 * <p> - discount description (e.g. "$10.00 off to Category: DL unlimited") </p>
 * <p> - discount value (e.g. 13) </p>
 * <p> - discount type (e.g. "USD", "%") </p>
 * <p> - application type of promotion (e.g. {"id": "LC_HD_202","type": "CatalogId"}) </p>
 */
public class PromotionDiscountTemplate extends DataModel {
    public String name;
    public String description;
    public Integer value;
    public String type;
    public DiscountNgbsDTO.ApplicableTo applicableTo;
}
