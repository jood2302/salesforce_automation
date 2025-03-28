package com.aquiva.autotests.rc.model.ngbs.dto.license;

import com.aquiva.autotests.rc.model.DataModel;
import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Data object with license info
 * for the requests to remove licenses from the NGBS account's package.
 */
@JsonInclude(value = NON_NULL)
public class RemovalRequestItem extends DataModel {
    public String licenseId;
    public String catalogId;
    public Integer qty;

    /**
     * Default no-arg constructor.
     * <br/>
     * Required for Jackson deserialization.
     */
    public RemovalRequestItem() {
    }

    /**
     * Create a new request to remove licenses from the NGBS account's package.
     *
     * @param licenseId unique ID for the license on the package (e.g. "13073777013")
     * @param qty       quantity of license (e.g. 1)
     */
    public RemovalRequestItem(String licenseId, Integer qty) {
        this.licenseId = licenseId;
        this.qty = qty;
    }
}
