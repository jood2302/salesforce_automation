package com.aquiva.autotests.rc.model.ngbs.dto.account;

import com.aquiva.autotests.rc.model.DataModel;
import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Data object with Account's Package details' summary to be used with NGBS API services.
 */
@JsonInclude(value = NON_NULL)
public class PackageSummary extends DataModel {
    public String id;
    public String status;
    public String catalogId;
    public String version;
    public String offerType;
    public String description;
    public String productLine;
}
