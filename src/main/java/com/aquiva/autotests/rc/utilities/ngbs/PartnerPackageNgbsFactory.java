package com.aquiva.autotests.rc.utilities.ngbs;

import com.aquiva.autotests.rc.model.ngbs.dto.partner.PartnerPackageDTO;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;

import java.time.Clock;

import static com.aquiva.autotests.rc.model.ngbs.dto.partner.PartnerPackageDTO.VersionsItem.ACTUAL_STATUS;

/**
 * Factory for generating instances of {@link PartnerPackageDTO} objects.
 */
public class PartnerPackageNgbsFactory {

    /**
     * Create a partner's package "object" with default values.
     * Such object could be used later in NGBS REST request for creating partner's package in NGBS.
     *
     * @return partner's package object to pass on in NGBS REST API request methods.
     */
    public static PartnerPackageDTO createPartnerPackage(Package testDataPackage) {
        var partnerPackage = new PartnerPackageDTO();
        partnerPackage.displayName = testDataPackage.name;

        var catalogPackage = new PartnerPackageDTO.CatalogPackage();
        catalogPackage.id = testDataPackage.id;
        catalogPackage.version = testDataPackage.version;
        partnerPackage.catalogPackage = catalogPackage;

        var version = new PartnerPackageDTO.VersionsItem();
        version.salesStartDate = Clock.systemUTC().instant().toString();
        version.status = ACTUAL_STATUS;
        version.version = 1;
        version.displayName = testDataPackage.name;
        partnerPackage.versions = new PartnerPackageDTO.VersionsItem[]{version};

        return partnerPackage;
    }
}
