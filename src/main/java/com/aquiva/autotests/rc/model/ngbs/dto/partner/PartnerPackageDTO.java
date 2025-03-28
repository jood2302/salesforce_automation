package com.aquiva.autotests.rc.model.ngbs.dto.partner;

import com.aquiva.autotests.rc.model.DataModel;
import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Data object to create a package for the NGBS Partner, for usage with NGBS API services.
 */
@JsonInclude(value = NON_NULL)
public class PartnerPackageDTO extends DataModel {
    public String displayName;
    public CatalogPackage catalogPackage;
    public VersionsItem[] versions;

    /**
     * Inner data structure that stores information about the Package from the NGBS Catalog
     * for usage with NGBS API services.
     * <br/>
     * E.g. "id": "1127005" and "version": "1" to link "RingCentral MVP Standard - v.1" package
     */
    @JsonInclude(value = NON_NULL)
    public static class CatalogPackage extends DataModel {
        public String id;
        public String version;
    }

    /**
     * Inner data structure that stores information about partner package's versions
     * for usage with NGBS API services.
     * <br/>
     * Can limit the sales dates, set display name, add additional labels, etc.
     */
    @JsonInclude(value = NON_NULL)
    public static class VersionsItem extends DataModel {
        //  For 'status' field
        public static final String ACTUAL_STATUS = "Actual";

        public Integer version;
        public String displayName;
        public String salesStartDate;
        public String status;
    }
}