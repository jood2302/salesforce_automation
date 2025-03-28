package com.aquiva.autotests.rc.model.ngbs.dto.contracts;

import com.aquiva.autotests.rc.model.DataModel;
import com.aquiva.autotests.rc.utilities.ngbs.ContractNgbsFactory;
import com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Data object with contract information for usage with NGBS API services.
 * <br/><br/>
 * Useful data structure for contract request objects and parsing responses
 * to/from NGBS API contract service (see {@link NgbsRestApiClient} for a reference).
 * <br/><br/>
 * Use {@link ContractNgbsFactory} to create quick instances of this DTO.
 */
@JsonInclude(value = NON_NULL)
public class ContractNgbsDTO extends DataModel {
    //  Constants for 'startBillingCycleNumber'
    public static final int CONTRACT_NOT_STARTED = -1;
    public static final int CONTRACT_TERMINATED = -2;

    public String id;
    public String createdAt;
    public String lastUpdated;
    public Integer startBillingCycleNumber;
    public String startDate;
    public String renewalDate;
    public String description;
    public Integer term;
    public Integer renewalTerm;
    public Boolean autoRenewal;
    public License[] licenses;

    /**
     * Inner data structure for License data object.
     * Represents data for specific contractual license.
     */
    @JsonInclude(value = NON_NULL)
    public static class License extends DataModel {
        public String id;
        public String catalogId;
        public Integer contractualQty;
    }

    /**
     * {@code true} if the contract is active.
     * <br/>
     * Note: for the Active contracts, the billing cycle number is greater than or equals 0
     * (could be 0, 1, 2, etc.)
     */
    @JsonIgnore
    public boolean isContractActive() {
        return startBillingCycleNumber >= 0;
    }
}
