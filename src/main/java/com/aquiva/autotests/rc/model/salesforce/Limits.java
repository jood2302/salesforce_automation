package com.aquiva.autotests.rc.model.salesforce;

import com.aquiva.autotests.rc.model.DataModel;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Data object with Org Limits information taken from Salesforce via REST API services.
 */
public class Limits extends DataModel {
    @JsonProperty("DataStorageMB")
    public LimitsDetails dataStorageMB;

    /**
     * Inner class for the limits details,
     * the same for all limits (e.g. max and remaining values of the given limit).
     */
    @JsonInclude(value = NON_NULL)
    public static class LimitsDetails extends DataModel {
        @JsonProperty("Max")
        public Double max;
        @JsonProperty("Remaining")
        public Double remaining;
    }
}
