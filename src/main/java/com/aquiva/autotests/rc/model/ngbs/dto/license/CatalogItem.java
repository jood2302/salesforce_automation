package com.aquiva.autotests.rc.model.ngbs.dto.license;

import com.aquiva.autotests.rc.model.DataModel;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.lang.String.format;

/**
 * Data object with license info
 * for the requests to change licenses on the NGBS account.
 */
@JsonInclude(value = NON_NULL)
public class CatalogItem extends DataModel {
    //  For 'billingState' field
    public static final String INITIAL_BILLING_STATE = "Initial";

    //  For 'billingCycleDuration' field
    public static final String ONETIME_BILLING_CYCLE = "OneTime";
    public static final String MONTHLY_BILLING_CYCLE = "Monthly";
    public static final String ANNUAL_BILLING_CYCLE = "Annual";

    public String catalogId;
    public String billingCycleDuration;
    public Integer qty;
    public Boolean batch;
    public String billingState;
    public List<CatalogItem> subItems;

    /**
     * Create a new Catalog Item for order with given quantity.
     *
     * @param dataName catalog ID of the license (e.g. for "DigitalLine Unlimited", it is "LC_DL-UNL_50")
     * @param quantity quantity of the main license to order
     */
    public static CatalogItem getItemFromTestData(String dataName, Integer quantity) {
        var catalogItem = JsonUtils.readConfigurationResource(
                format("data/licenseToOrder/%s.json", dataName),
                CatalogItem.class);
        catalogItem.qty = quantity;

        return catalogItem;
    }
}
